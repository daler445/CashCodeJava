package tj.epic.cashcode;

import jssc.SerialPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class InitCashCodeTest {
	@Mock
	private SerialPort serialPort;

	private CashCodeSM cashCode;

	private final String portName = "COM1";
	private final int baudRate = 9600;
	private final int dataBits = 8;
	private final int stopBits = 1;
	private final int parity = 0;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		cashCode = new CashCodeSM();
		cashCode.setSerialPort(serialPort);
	}

	@Test
	public void testInit_success() throws Exception {
		when(serialPort.openPort()).thenReturn(true);

		assertDoesNotThrow(() -> cashCode.init(portName, baudRate, dataBits, stopBits, parity));

		verify(serialPort).openPort();
		verify(serialPort).setParams(baudRate, dataBits, stopBits, parity);
	}
}
