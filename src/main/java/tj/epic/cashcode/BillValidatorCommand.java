package tj.epic.cashcode;

public enum BillValidatorCommand {
	ACK(0x00),
	RESET(0x30),
	GET_STATUS(0x31),
	SET_SECURITY(0x32),
	POLL(0x33),
	ENABLE_BILL_TYPES(0x34),
	STACK(0x35),
	RETURN(0x36),
	IDENTIFICATION(0x37),
	HOLD(0x38),
	NAK(0xFF);

	public final int code;

	BillValidatorCommand(int i) {
		this.code = i;
	}

	public int getCode() {
		return code;
	}
}
