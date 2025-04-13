# Serial_Talker_Logger

A modern serial communication tool built using Java 17 and the jssc library. This application provides a clean, efficient interface for serial device communication through a modern Swing-based GUI. It supports real-time data logging and visualization, making it an ideal choice for developers and engineers working with serial devices.

## Features
- **Modern Java Implementation:** Built with Java 17, utilizing modern language features and best practices
- **Efficient Serial Communication:** Fast and reliable serial device communication using jssc
- **Real-Time Port Detection:** Automatic detection and updating of available serial ports
- **Robust Error Handling:** Comprehensive error handling and user feedback
- **Clean UI Design:** Modern, responsive user interface with intuitive controls
- **Data Logging:** Save communication logs to files for analysis
- **Configurable Settings:** Easy configuration of serial parameters:
  - Baud Rate
  - Data Bits
  - Stop Bits
  - Parity

## Requirements
- Java 17 or higher
- Maven 3.6 or higher
- jssc library (automatically managed by Maven)

## Getting Started

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/geoffop/Serial_Talker_Logger.git
   cd Serial_Talker_Logger
   ```

2. **Build the Project:**
   ```bash
   mvn clean package
   ```

3. **Run the Application:**
   ```bash
   java -Djava.library.path=target/lib -jar target/serialtalkerlogger-1.0.jar
   ```

## Usage

1. Launch the application
2. Select your serial port from the dropdown list
3. Click "Connect" to establish communication
4. Use the provided buttons to send test messages or save logs
5. Access Settings through the menu to configure serial parameters

## Development

The project uses Maven for dependency management and building. Key dependencies:

- jssc: For serial port communication
- JUnit: For unit testing

To modify the project:

1. Make your changes in the `src/main/java` directory
2. Run tests: `mvn test`
3. Build: `mvn package`

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request
