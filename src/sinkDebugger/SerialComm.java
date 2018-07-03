/**
 * SerialComm.java
 * Purpose: This class handles all UART communications in a modular way so it can be used from any GUI we design.
 * Notes: This class should never refer to any outside GUI element. If a method needs to change a status label, progress bar, etc., pass it in as a parameter
 * 		  
 */

package sinkDebugger;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import purejavacomm.CommPortIdentifier;
import purejavacomm.PortInUseException;
import purejavacomm.PureJavaIllegalStateException;
import purejavacomm.SerialPort;
import purejavacomm.UnsupportedCommOperationException;

public class SerialComm {

	// Input and Output Streams of the serial port, input stream must be buffered to
	// prevent data loss due to buffer overflows, DO NOT USE a BufferedReader, it
	// will encode bytes via UTF-8
	private BufferedInputStream inputStream;
	private OutputStream outputStream;

	// Serial port identifiers for opening and the serial port
	private CommPortIdentifier portId;
	private SerialPort serialPort;
	private String serialPortName;

	// Flags that track object/process states
	private boolean dataStreamsInitialized = false;
	private boolean remoteTestActive = false;

	// Constructor not used to initialize anything right now.
	public SerialComm() {
	}

	/**
	 * Builds a list the names of all the serial ports to place in the combo box
	 * 
	 * @param evt
	 *            event pasted in by any button or action that this method was
	 *            called by (method of passing info related to the source)
	 */
	public ArrayList<String> findPorts() {
		// Fills the portEnum data structure (functions like arrayList) with ports (data
		// type that encapsulates the name and hardware interface info)
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();

		// Stores the names of the ports
		ArrayList<String> portNames = new ArrayList<String>();

		// Iterate through each port object in the portEnumList and stores the name of
		// the port in the portNames array
		while (portList.hasMoreElements()) { // adds the serial ports to a string array
			CommPortIdentifier portIdentifier = portList.nextElement();
			portNames.add(portIdentifier.getName());
		}

		// If at least 1 serial port is found, fill the combo box with all the known
		// port names. Otherwise, notify the user that there are no visible dongles.
		if (portNames.size() > 0) {
			return portNames;
		}

		return null;

	}

	/**
	 * Opens serial port with the name passed in as a parameter in addition to
	 * initializing input and output streams.
	 * 
	 * @param commPortID
	 *            Name of comm port that will be opened
	 */
	public boolean openSerialPort(String commPortID) throws IOException, PortInUseException {
		// Creates a list of all the ports that are available of type Enumeration (data
		// structure that can hold several info fields such as ID, hardware interface
		// info, and other info used by the PC
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();

		// Iterates through all ports on the ports on the port list
		while (portList.hasMoreElements()) {

			// Set the temporary port to the current port that is being iterated through
			CommPortIdentifier tempPortId = (CommPortIdentifier) portList.nextElement();

			// Executes if the temporary port has the same name as the one selected by the
			// user
			if (tempPortId.getName().equals(commPortID)) {

				// If it does match, then assign the portID variable so the desired port will be
				// opened later
				portId = tempPortId;

				// break the while loop
				break;
			}
		}

		// Open the serial port with a 2 second timeout
		serialPort = (SerialPort) portId.open("portHandler", 2000);

		// Create a new buffered reader so we can define the buffer size to prevent a
		// buffer overflow (explicitly defined in the configureForImport() method)
		inputStream = new BufferedInputStream(serialPort.getInputStream(), 8192);

		// Assign the output stream to the output stream of the serial port (no need for
		// a buffer as far as we know)
		outputStream = serialPort.getOutputStream();

		// Set flag so program knows that the data streams were initialized
		dataStreamsInitialized = true;

		return true;

	}

	/**
	 * Closes serial port and updates GUI labels/ software flags
	 */
	public boolean closeSerialPort() {
		// If the disconnect button is pressed: disconnects from the serial port and
		// resets the UI
		if (serialPort != null) {

			// Close the serial port
			serialPort.close();

			// Let the whole class know that the data streams are no longer initialized
			dataStreamsInitialized = false;
			return true;

		}
		// Method failed so return false
		return false;
	}

	/**
	 * Clears the input stream buffer
	 */
	public boolean clearInputStream() throws IOException {

		// Executes if the data streams are currently initialized (prevents null pointer
		// exception)
		if (dataStreamsInitialized) {
			// Executes while there is still data in the input stream buffer
			while (inputStream.available() > 0) {
				// Read a value from the buffer and don't store it, just throw it away
				inputStream.read();
			}
			return true;

		}
		// Method failed so return false
		return false;
	}

	/**
	 * Configures the serial port and input/output streams for the handshake
	 * sequences (most important parameter is the baud rate)
	 * 
	 * @return boolean that allows for easy exiting of the method in addition to
	 *         notifying the caller that if it was successful
	 */
	public boolean init() throws IOException, PortInUseException, UnsupportedCommOperationException {
		// Close the current serial port if it is open (Must be done for dashboard to
		// work properly for some reason, do not delete)
		if (dataStreamsInitialized) {
			serialPortName = serialPort.getName();
			serialPort.close();
		}

		// Reopen serial port
		if (serialPortName != null) {
			openSerialPort(serialPortName);
		} else {
			openSerialPort(portId.getName());
		}

		// Configure the serial port for 38400 baud for low speed handshakes
		serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		// Assign the output stream variable to the serial port's output stream
		outputStream = serialPort.getOutputStream();
		// Assign the input stream variable to the serial port's input stream via a
		// buffered reader so we have the option to specify the buffer size
		inputStream = new BufferedInputStream(serialPort.getInputStream(), 8192);
		dataStreamsInitialized = true;

		// Return true to exit the method and notify the caller that the method was
		// successful
		return true;
	}
	

	public int[] readData() throws IOException {
		int[] data = new int[5];

		if (dataStreamsInitialized) {
			//System.out.println("boi");
			for (int preambleChecker = 0; preambleChecker < 3;) {
				int temp = inputStream.read();
				//System.out.println(temp);
				if(temp == 12) {
					return null;
				}
				
				if (preambleChecker == 0) {
					if (temp == 16) {
						preambleChecker++;
					} else {
						preambleChecker = 0;
					}
				}
				else if (preambleChecker == 1) {
					if (temp == 64) {
						preambleChecker++;
					} else {
						preambleChecker = 0;
					}
				}
				else if (preambleChecker == 2) {
					if (temp == 14) {
						preambleChecker++;
					} else {
						preambleChecker = 0;
					}
				}

			}
			//System.out.println("Found ya boi");
			char temp = 0;

			 for(int i = 0; i < 4; i++) {
				int numericValue = 0;
				int digiCntr = 0;
				do {
					temp = (char)inputStream.read();
					if (temp >= 48 && temp <= 57) {
						if(digiCntr == 0)
							numericValue = Character.getNumericValue(temp);
						else 
							numericValue = Character.getNumericValue(temp) + (numericValue * 10);
						digiCntr++;
						
					}
				} while((temp >= 48 && temp <= 57) || digiCntr == 0);
				data[i] = numericValue;
			}
			
			while(inputStream.read() != 16) {}
			inputStream.read();
			int loopCntr = 0;
			int numericValue = 0;
			do {
				temp = (char)inputStream.read();
				if((temp >= 48 && temp <= 57)) {
					if(loopCntr == 0)
						numericValue = Character.getNumericValue(temp);
					else 
						numericValue = Character.getNumericValue(temp) + (numericValue * 10);
				}
				loopCntr++;
			} while(temp != 32);
			
			data[4] = numericValue;
			
		}
		return data;
	}

	public BufferedInputStream getInputStream() {
		return inputStream;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public boolean streamsInitialized() {
		return dataStreamsInitialized;
	}

}
