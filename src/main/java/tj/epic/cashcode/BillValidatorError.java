package tj.epic.cashcode;

public enum BillValidatorError {
	NONE,
	ILLEGAL_COMMAND,
	DROP_CASSETTE_FULL,
	DROP_CASSETTE_OUT_OF_POSITION,
	VALIDATOR_JAMMED,
	DROP_CASSETTE_JAMMED,
	CHEATED,
	PAUSE,
	STACK_MOTOR_FAILURE,
	TRANSPORT_MOTOR_SPEED_FAILURE,
	TRANSPORT_MOTOR_FAILURE,
	ALIGNING_MOTOR_FAILURE,
	INITIAL_CASSETTE_STATUS_FAILURE,
	OPTIC_CANAL_FAILURE,
	MAGNETIC_CANAL_FAILURE,
	CAPACITANCE_CANAL_FAILURE,
	GENERIC_FAILURE,
}
