package tj.epic.cashcode;

public interface CashCodeEvents {
	void onAccept();
	void onReject();
	void onEscrowPosition();
	void onStack();
	void onBillStack(int value);
	void onReturn();
	void onBillReturned();
	void onDropCassetteOutOfPosition();
	void onCassetteInitialize();
}
