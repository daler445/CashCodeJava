package tj.epic.cashcode;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tj.epic.cashcode.exceptions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CashCodeSM implements CashCode {
	private static final int DELAY_TIME_MS = 528;

	// device states
	private BillCassetteStatus cassetteStatus = BillCassetteStatus.ESTABLISHED;
	private BillValidatorError billValidatorErrorState = BillValidatorError.NONE;

	private boolean isConnected = false;
	private boolean isPoweredUp = false;
	private boolean isBillEnabled = false;

	// should store values only for one session
	private final List<Integer> insertedMoney = new ArrayList<>();

	private SerialPort serialPort;
	private Logger logger = LoggerFactory.getLogger(CashCodeSM.class);

	@Override
	public void init(String portName, int baudRate, int dataBits, int stopBits, int parity) throws CashCodeException {
		logger.info("Initializing");
		if (serialPort == null) {
			logger.debug("Serial port is not set, creating new one. Port name: %s".formatted(portName));
			serialPort = new SerialPort(portName);
		}

		try {
			logger.debug("Opening port");
			serialPort.openPort();

			logger.debug("Setting params. Baud rate: %s, data bits: %s stop bits: %s parity: %s".formatted(baudRate, dataBits, stopBits, parity));
			serialPort.setParams(baudRate, dataBits, stopBits, parity);
		} catch (SerialPortException e) {
			logger.error(e.getMessage());
			throw new PortException("Port error: " + e.getMessage());
		}
		setConnected(true);
	}

	@Override
	public void disconnect() throws CashCodeException {
		if (!isConnected()) {
			logger.debug("Already disconnected");
			return;
		}

		if (!serialPort.isOpened()) {
			logger.warn("Port is already closed");
			setConnected(false);
			return;
		}

		try {
			logger.debug("Closing port");
			serialPort.closePort();
			logger.debug("Port closed");
		} catch (SerialPortException e) {
			logger.error(e.getMessage());
			throw new DisconnectException("Disconnect error: " + e.getMessage());
		}

		setConnected(false);
	}

	@Override
	public void powerUp() throws CashCodeException {
		if (!isConnected()) {
			logger.error("Port is not connected");
			throw new PortNotConnectedException("Port is not connected. Init method should be called first");
		}

		try {
			sendPackage(BillValidatorCommand.POLL, new int[]{});
			delay();

			int[] pollResultPowerUp = readPackage();

			Optional<BillValidatorError> powerUpError = getDeviceError(pollResultPowerUp);
			if (powerUpError.isPresent()) {
				billValidatorErrorState = powerUpError.get();
				sendPackage(BillValidatorCommand.NAK, new int[]{});
				throw new PowerUpException("Power Up failed");
			}

			sendPackage(BillValidatorCommand.ACK, new int[]{});
			delay();

			sendPackage(BillValidatorCommand.RESET, new int[]{});
			delay();

			sendPackage(BillValidatorCommand.ENABLE_BILL_TYPES, new int[]{255, 255, 255, 0, 0, 0});
			delay();

			sendPackage(BillValidatorCommand.GET_STATUS, new int[]{});
			delay();

			// check, if not failed
			int[] status = readPackage();
			Optional<BillValidatorError> statusError = getDeviceError(status);
			if (statusError.isPresent()) {
				billValidatorErrorState = statusError.get();
				sendPackage(BillValidatorCommand.NAK, new int[]{});
				throw new PowerUpException("Power Up failed");
			}

			sendPackage(BillValidatorCommand.SET_SECURITY, new int[]{0x00, 0x00, 0x00});
			delay();

			int[] securityModeStatus = readPackage();
			Optional<BillValidatorError> securityModeError = getDeviceError(securityModeStatus);
			if (securityModeError.isPresent()) {
				billValidatorErrorState = securityModeError.get();
				sendPackage(BillValidatorCommand.NAK, new int[]{});
				throw new SecurityModeException("Security Mode Error");
			}

			sendPackage(BillValidatorCommand.IDENTIFICATION, new int[]{});
			delay();

			sendPackage(BillValidatorCommand.ACK, new int[]{});
			delay();

			sendPackage(BillValidatorCommand.POLL, new int[]{});
			delay();

			int[] pollStatus = readPackage();
			Optional<BillValidatorError> pollError = getDeviceError(pollStatus);
			if (pollError.isPresent()) {
				billValidatorErrorState = pollError.get();
				sendPackage(BillValidatorCommand.NAK, new int[]{});
				throw new PowerUpException("Power Up failed");
			}

			sendPackage(BillValidatorCommand.ACK, new int[]{});
			delay();

			setBillEnabled(true);
			setPoweredUp(true);
			logger.debug("Device powered up successfully");
		} catch (SerialPortException e) {
			logger.error("Failed powering up a device", e);
			throw new GeneralCashCodeException("CashCode error: " + e.getMessage());
		}
	}

	@Override
	public void powerDown() throws CashCodeException {
		if (!isConnected) {
			logger.debug("Device is not connected");
			return;
		}

		try {
			sendPackage(BillValidatorCommand.RESET, new int[]{});
			delay();

			sendPackage(BillValidatorCommand.POLL, new int[]{0, 0, 0, 0, 0, 0});
			delay();

			setPoweredUp(false);
			logger.debug("Powered down successfully");
		} catch (SerialPortException e) {
			logger.error("Failed powering down a device", e);
			throw new PowerDownException("Power down error: " + e.getMessage());
		}
	}

	@Override
	public void startPolling(CashCodeEvents eventListener) throws CashCodeException {
		logger.debug("Starting polling");
		insertedMoney.clear();

		if (!isConnected()) {
			throw new InvalidCashCodeStateException("Device is not connected");
		}

		if (!isPoweredUp()) {
			throw new InvalidCashCodeStateException("Device is not powered up");
		}

		if (cassetteStatus != BillCassetteStatus.ESTABLISHED) {
			throw new InvalidCashCodeStateException("Device is not established");
		}

		while (isBillEnabled()) {
			try {
				sendPackage(BillValidatorCommand.POLL, new int[]{});
				delay();

				int[] pollResult = readPackage();
				delay();

				// if there is no third byte, no event happened. just skipping the iteration
				if (pollResult.length < 4) {
					continue;
				}

				// if is idling, skipping the iteration. nothing happened
				if (pollResult[3] == 0x14) {
					continue;
				}

				switch (pollResult[3]) {
					case 0x13 -> {
						logger.debug("E: Cassette initialized");
						cassetteStatus = BillCassetteStatus.ESTABLISHED;
						eventListener.onCassetteInitialize();
					}
					case 0x15 -> {
						logger.debug("E: Accepted");
						eventListener.onAccept();
					}
					case 0x17 -> {
						logger.debug("E: Stacked");
						eventListener.onStack();
					}
					case 0x18 -> {
						logger.debug("E: Returning");
						eventListener.onReturn();
					}
					case 0x42 -> {
						logger.debug("E: Cassette removed");
						cassetteStatus = BillCassetteStatus.REMOVED;
						eventListener.onDropCassetteOutOfPosition();
					}
					case 0x80 -> {
						logger.debug("E: Escrow position");
						eventListener.onEscrowPosition();
					}
					case 0x81 -> {
						logger.debug("E: Banknote inserted");
						banknoteInserted(pollResult[4]);
						eventListener.onBillStack(pollResult[4]);
					}
					case 0x82 -> {
						logger.debug("E: Returned");
						eventListener.onBillReturned();
					}
					case 0x1C -> {
						logger.debug("E: Rejected");
						eventListener.onReject();
					}
				}

				sendPackage(BillValidatorCommand.ACK, new int[]{});
				delay();
			} catch (SerialPortException e) {
				logger.error(e.getMessage());
				throw new GeneralCashCodeException("CashCode error: " + e.getMessage());
			}
		}
	}

	@Override
	public void stopPolling() throws CashCodeException {
		logger.debug("Stopping polling");
		setBillEnabled(false);
		try {
			sendPackage(BillValidatorCommand.ENABLE_BILL_TYPES, new int[]{0, 0, 0, 0, 0, 0});
			delay();
			int[] stopStatus = readPackage();
			if (stopStatus.length < 4 || stopStatus[3] != 0x00) {
				logger.error("CashCode error. Unknown error");
				throw new GeneralCashCodeException("CashCode error. Unknown error");
			}
			logger.debug("Stopped polling");
		} catch (SerialPortException e) {
			logger.error(e.getMessage());
			throw new GeneralCashCodeException("CashCode error: " + e.getMessage());
		}
	}

	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public List<Integer> getInsertedBanknotes() {
		return this.insertedMoney;
	}

	@Override
	public BillValidatorError getError() {
		return this.billValidatorErrorState;
	}

	public void setSerialPort(SerialPort serialPort) {
		logger.debug("Setting serial port to %s".formatted(serialPort));
		this.serialPort = serialPort;
	}

	private void banknoteInserted(int code) {
		this.insertedMoney.add(code);
	}

	private boolean isConnected() {
		return isConnected;
	}

	private void setConnected(boolean connected) {
		logger.debug("Setting connected = %s".formatted(connected));
		isConnected = connected;
	}

	private boolean isPoweredUp() {
		return isPoweredUp;
	}

	private void setPoweredUp(boolean poweredUp) {
		logger.debug("Setting powered up = %s".formatted(poweredUp));
		isPoweredUp = poweredUp;
	}

	private boolean isBillEnabled() {
		return isBillEnabled;
	}

	private void setBillEnabled(boolean billEnabled) {
		logger.debug("Setting bill enabled = %s".formatted(billEnabled));
		isBillEnabled = billEnabled;
	}

	private void sendPackage(BillValidatorCommand command, int[] data) throws SerialPortException {
		logger.debug("--> C-%s D-%s".formatted(command.name(), Arrays.toString(data)));
		serialPort.writeIntArray(buildPackage(command.getCode(), data));
	}

	private int[] readPackage() throws SerialPortException {
		return serialPort.readIntArray();
	}

	private Optional<BillValidatorError> getDeviceError(int[] receivedBytes) {
		if (receivedBytes.length < 4) {
			return Optional.of(BillValidatorError.GENERIC_FAILURE);
		}

		return switch (receivedBytes[3]) {
			case 0x30 -> Optional.of(BillValidatorError.ILLEGAL_COMMAND);
			case 0x41 -> Optional.of(BillValidatorError.DROP_CASSETTE_FULL);
			case 0x42 -> Optional.of(BillValidatorError.DROP_CASSETTE_OUT_OF_POSITION);
			case 0x43 -> Optional.of(BillValidatorError.VALIDATOR_JAMMED);
			case 0x44 -> Optional.of(BillValidatorError.DROP_CASSETTE_JAMMED);
			case 0x45 -> Optional.of(BillValidatorError.CHEATED);
			case 0x46 -> Optional.of(BillValidatorError.PAUSE);
			case 0x47 -> {
				if (receivedBytes.length < 5) {
					yield Optional.of(BillValidatorError.GENERIC_FAILURE);
				}

				yield switch (receivedBytes[4]) {
					case 0x50 -> Optional.of(BillValidatorError.STACK_MOTOR_FAILURE);
					case 0x51 -> Optional.of(BillValidatorError.TRANSPORT_MOTOR_SPEED_FAILURE);
					case 0x52 -> Optional.of(BillValidatorError.TRANSPORT_MOTOR_FAILURE);
					case 0x53 -> Optional.of(BillValidatorError.ALIGNING_MOTOR_FAILURE);
					case 0x54 -> Optional.of(BillValidatorError.INITIAL_CASSETTE_STATUS_FAILURE);
					case 0x55 -> Optional.of(BillValidatorError.OPTIC_CANAL_FAILURE);
					case 0x56 -> Optional.of(BillValidatorError.MAGNETIC_CANAL_FAILURE);
					case 0x5F -> Optional.of(BillValidatorError.CAPACITANCE_CANAL_FAILURE);
					default -> Optional.of(BillValidatorError.GENERIC_FAILURE);
				};
			}
			default -> Optional.empty();
		};
	}

	private void delay() {
		try {
			Thread.sleep(DELAY_TIME_MS);
		} catch (InterruptedException ignored) {
		}
	}

	private int[] buildPackage(int command, int[] data) {
		int length = data.length + 6;
		int[] commandArr = new int[256];
		commandArr[0] = 0x02;       //sync
		commandArr[1] = 0x03;       //valid address
		commandArr[2] = length;     //length
		commandArr[3] = command;    //command

		if (data.length != 0) {
			int i = 4, d = 0;
			while (d != data.length) {
				commandArr[i] = data[d];
				i += 1;
				d += 1;
			}
		}
		int[] crcPacket = Arrays.copyOfRange(commandArr, 0, length - 2);
		int crcValue = getCrc16(crcPacket);
		commandArr[length - 1] = (crcValue >> 8) & 0xFF;
		commandArr[length - 2] = crcValue & 0xFF;
		return Arrays.copyOfRange(commandArr, 0, length);
	}

	private int getCrc16(int[] arr) {
		int i, tmpCrc = 0;
		byte j;
		for (i = 0; i <= arr.length - 1; i++) {
			tmpCrc ^= arr[i];
			for (j = 0; j <= 7; j++) {
				if ((tmpCrc & 0x0001) != 0) {
					tmpCrc >>= 1;
					tmpCrc ^= 0x08408;    // 0x08408 = Polynomial
				} else {
					tmpCrc >>= 1;
				}
			}
		}
		return tmpCrc;
	}
}
