package tj.epic.cashcode;

import jssc.SerialPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tj.epic.cashcode.exceptions.PortNotConnectedException;
import tj.epic.cashcode.exceptions.PowerUpException;
import tj.epic.cashcode.exceptions.SecurityModeException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public final class PowerUpCashCodeTest {
	@Mock
	private SerialPort serialPort;

	private CashCodeSM cashCode;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		cashCode = new CashCodeSM();
		cashCode.setSerialPort(serialPort);
	}

	@Test
	public void testPowerUp() throws Exception {
		when(serialPort.readIntArray()).thenReturn(new int[]{0x00, 0x00, 0x00, 0x00});
		when(serialPort.writeIntArray(any(int[].class))).thenReturn(true);

		TestsHelper.changePrivateFieldValue(cashCode, "isConnected", true);
		TestsHelper.changePrivateFieldValue(cashCode, "isBillEnabled", false);
		TestsHelper.changePrivateFieldValue(cashCode, "isPoweredUp", false);

		cashCode.powerUp();

		verify(serialPort, times(10)).writeIntArray(any(int[].class));
		verify(serialPort, times(4)).readIntArray();

		assertTrue((boolean)TestsHelper.getPrivateFieldValue(cashCode, "isBillEnabled"));
		assertTrue((boolean)TestsHelper.getPrivateFieldValue(cashCode, "isPoweredUp"));
	}

	@Test
	public void testPowerUp_PortNotConnectedException() {
		TestsHelper.changePrivateFieldValue(cashCode, "isConnected", false);
		assertThrows(PortNotConnectedException.class, () -> cashCode.powerUp());
	}

	@Test
	public void testPowerUp_PowerUpException() throws Exception {
		when(serialPort.readIntArray()).thenReturn(new int[]{0x30});

		TestsHelper.changePrivateFieldValue(cashCode, "isConnected", true);

		assertThrows(PowerUpException.class, () -> cashCode.powerUp());
		verify(serialPort, times(2)).writeIntArray(any(int[].class));
		verify(serialPort, times(1)).readIntArray();
	}

	@Test
	public void testPowerUp_SecurityModeException() throws Exception {
		when(serialPort.readIntArray()).thenReturn(
				new int[]{0x00, 0x00, 0x00, 0x00},
				new int[]{0x00, 0x00, 0x00, 0x00},
				new int[]{0x00, 0x00, 0x00, 0x47, 0x50}
		);
		TestsHelper.changePrivateFieldValue(cashCode, "isConnected", true);

		assertThrows(SecurityModeException.class, () -> cashCode.powerUp());
		verify(serialPort, times(7)).writeIntArray(any(int[].class));
		verify(serialPort, times(3)).readIntArray();
	}
}
