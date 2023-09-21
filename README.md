# Cash Code Java Driver
## Description

The CashCode SM driver is a vital software component that facilitates the seamless integration and operation of CashCode SM bill acceptors across diverse applications. It offers a standardized communication interface, adept data parsing, rigorous error management, and customizable configuration options

## Getting Started

### Dependencies

* Java 17
* JSSC

### Initialization
```java
CashCode cc = new CashCodeSM();
cc.init("COM1", 9600, 8, 1, 0);
// ...
```

### Methods
* `void init(String portName, int baudRate, int dataBits, int stopBits, int parity)`: Initiates the connection with the CashCode device.
* `void disconnect()`: Terminates the connection with the CashCode device.
* `void powerUp()`: Activates the CashCode device, powering it up.
* `void powerDown()`: Deactivates the CashCode device, powering it down.
* `void startPolling(CashCodeEvents eventListener)`: Commences the process of monitoring and capturing incoming currency inputs.
* `void stopPolling()`: Ceases the monitoring of currency inputs.
* `List<Integer> getInsertedBanknotes()`: Retrieves a list of currently inserted banknotes.
* `BillValidatorError getError()`: Provides information regarding the most recent error reported by the CashCode device.

### CashCode Events
* `void onAccept()`: This event is triggered when a banknote is successfully accepted by the validator.
* `void onReject()`: This event is triggered when a banknote is rejected by the validator.
* `void onEscrowPosition()`: This state is reported when the validator encounters an escrow position, with the Bill Value field indicating zero for an unknown value.
* `void onStack()`: The validator remains in this state while it moves a banknote from the escrow position toward a fully-secured position, passing all internal sensors. The validator will remain in this state until the banknote is successfully stacked or a jam occurs.
* `void onBillStack(int value)`: This event is triggered when a banknote is accepted, providing the value of the accepted banknote.
* `void onReturn()`: In this state, the banknote is being returned to the customer, as directed by the controller in response to an Escrow message.
* `void onBillReturned()`: This event is triggered when the process initiated by onReturn has been completed.
* `void onDropCassetteOutOfPosition()`: This event is triggered when the recycling cassette is detected as being out of its designated position or when a communication failure occurs.
* `void onCassetteInitialize()`: This event is triggered when the cassette is restored to its proper position after having been out of position.

## Version History

* 1.0
    * Initial Release

## License

This project is licensed under the FreeBSD License License - see the LICENSE.md file for details

## Links
* [www.cashcode.com](https://www.cashcode.com)

## TODO
* Create maven package