package tj.epic.cashcode;

import org.slf4j.Logger;
import tj.epic.cashcode.exceptions.CashCodeException;

import java.util.List;

public interface CashCode {
	void init(String portName, int baudRate, int dataBits, int stopBits, int parity) throws CashCodeException;
	void disconnect() throws CashCodeException;
	void powerUp() throws CashCodeException;
	void powerDown() throws CashCodeException;
	void startPolling(CashCodeEvents eventHandler) throws CashCodeException;
	void stopPolling() throws CashCodeException;
	void setLogger(Logger logger);
	List<Integer> getInsertedBanknotes();
	BillValidatorError getError();
}
