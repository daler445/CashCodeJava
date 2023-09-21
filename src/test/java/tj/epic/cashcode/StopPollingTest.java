package tj.epic.cashcode;

import jssc.SerialPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class StopPollingTest {
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
	public void testStopPolling() throws Exception {
		TestsHelper.changePrivateFieldValue(cashCode, "isBillEnabled", true);

		when(serialPort.readIntArray()).thenReturn(new int[]{0x00, 0x00, 0x00, 0x00});

		cashCode.stopPolling();
		verify(serialPort).readIntArray();
	}
}
