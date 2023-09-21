package tj.epic.cashcode;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tj.epic.cashcode.exceptions.GeneralCashCodeException;
import tj.epic.cashcode.exceptions.InvalidCashCodeStateException;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StartPollingTest {
	@Mock
	private SerialPort serialPort;

	@Mock
	private CashCodeEvents eventListener;

	private CashCodeSM cashCode;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		cashCode = new CashCodeSM();
		cashCode.setSerialPort(serialPort);
	}

	@Test
	public void testStartPolling_cassetteInitialize() throws Exception {
		TestsHelper.changePrivateFieldValue(cashCode, "isConnected", true);
		TestsHelper.changePrivateFieldValue(cashCode, "isBillEnabled", true);
		TestsHelper.changePrivateFieldValue(cashCode, "isPoweredUp", true);

		CountDownLatch latch = new CountDownLatch(1);

		when(serialPort.readIntArray()).thenAnswer(invocation -> {
			latch.countDown();
			return new int[]{0x00, 0x00, 0x00, 0x13};
		});

		Thread pollingThread = new Thread(() -> {
			try {
				cashCode.startPolling(eventListener);
			} catch (Exception e) {
				fail(e);
			}
		});
		pollingThread.start();

		latch.await();

		CountDownLatch cassetteInitializeLatch = new CountDownLatch(1);
		doAnswer(invocation -> {
			cassetteInitializeLatch.countDown();
			return null;
		}).when(eventListener).onCassetteInitialize();
		cassetteInitializeLatch.await();

		verify(eventListener).onCassetteInitialize();
		verify(serialPort, atLeastOnce()).writeIntArray(any(int[].class));
		verify(serialPort, atLeastOnce()).readIntArray();

		when(serialPort.readIntArray()).thenReturn(new int[]{0x00, 0x00, 0x00, 0x00});
		cashCode.stopPolling();

		pollingThread.join();
	}

	@Test
	public void testStartPolling_onBillStack() throws Exception {
		TestsHelper.changePrivateFieldValue(cashCode, "isConnected", true);
		TestsHelper.changePrivateFieldValue(cashCode, "isBillEnabled", true);
		TestsHelper.changePrivateFieldValue(cashCode, "isPoweredUp", true);

		CountDownLatch latch = new CountDownLatch(1);

		when(serialPort.readIntArray()).thenAnswer(invocation -> {
			latch.countDown();
			return new int[]{0x00, 0x00, 0x00, 0x81, 0x50};
		});

		Thread pollingThread = new Thread(() -> {
			try {
				cashCode.startPolling(eventListener);
			} catch (Exception e) {
				fail(e);
			}
		});
		pollingThread.start();

		latch.await();

		CountDownLatch onBillStackLatch = new CountDownLatch(1);
		doAnswer(invocation -> {
			onBillStackLatch.countDown();
			return null;
		}).when(eventListener).onBillStack(0x50);
		onBillStackLatch.await();

		verify(eventListener).onBillStack(0x50);
		verify(serialPort, atLeastOnce()).writeIntArray(any(int[].class));
		verify(serialPort, atLeastOnce()).readIntArray();

		when(serialPort.readIntArray()).thenReturn(new int[]{0x00, 0x00, 0x00, 0x00});
		cashCode.stopPolling();

		pollingThread.join();

		List<Integer> banknotes = cashCode.getInsertedBanknotes();
		assertEquals(1, banknotes.size());
		assertEquals(0x50, banknotes.get(0));
	}

	@Test
	public void testStartPolling_InvalidCashCodeStateException() {
		TestsHelper.changePrivateFieldValue(cashCode, "isConnected", false);
		TestsHelper.changePrivateFieldValue(cashCode, "isBillEnabled", true);
		TestsHelper.changePrivateFieldValue(cashCode, "isPoweredUp", true);

		assertThrows(InvalidCashCodeStateException.class, () -> cashCode.startPolling(eventListener));
	}

	@Test
	public void testStartPolling_GeneralCashCodeException() throws Exception {
		when(serialPort.writeIntArray(any())).thenThrow(SerialPortException.class);

		TestsHelper.changePrivateFieldValue(cashCode, "isConnected", true);
		TestsHelper.changePrivateFieldValue(cashCode, "isBillEnabled", true);
		TestsHelper.changePrivateFieldValue(cashCode, "isPoweredUp", true);

		assertThrows(GeneralCashCodeException.class, () -> cashCode.startPolling(eventListener));
		verify(serialPort, times(1)).writeIntArray(any(int[].class));
		verify(serialPort, times(0)).readIntArray();
	}
}
