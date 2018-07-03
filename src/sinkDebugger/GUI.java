package sinkDebugger;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import purejavacomm.PortInUseException;

import java.awt.ScrollPane;
import java.awt.GridLayout;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Font;

public class GUI extends JFrame {

	private JPanel contentPane;
	public SerialComm serialHandler;
	private JButton disconnectButton;
	private JComboBox commPortCombobox;
	public JLabel generalStatusLabel;
	private JTextArea dataDsp;
	private ArrayList<String> data = new ArrayList<String>();
	private ArrayList<String> formattedData = new ArrayList<String>();
	private JScrollPane scrollPane;
	private JScrollPane sp;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI frame = new GUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public GUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 633, 538);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.NORTH);
		panel.setLayout(new GridLayout(1, 3, 0, 0));

		JButton refreshPortButton = new JButton("Refresh Port List");
		refreshPortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				updateCommPortComboBox();
			}
		});
		refreshPortButton.setBorder(null);
		panel.add(refreshPortButton);

		commPortCombobox = new JComboBox();
		commPortCombobox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				portSelectedHandler();
			}
		});
		commPortCombobox.setEnabled(false);
		panel.add(commPortCombobox);

		disconnectButton = new JButton("Disconnect");
		disconnectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				disconnectButtonHandler();
			}
		});
		disconnectButton.setForeground(Color.BLACK);
		disconnectButton.setBorder(null);
		panel.add(disconnectButton);

		generalStatusLabel = new JLabel("      ");
		contentPane.add(generalStatusLabel, BorderLayout.SOUTH);
		

		
				dataDsp = new JTextArea();
				dataDsp.setFont(new Font("Monospaced", Font.PLAIN, 14));
				sp = new JScrollPane(dataDsp);
				contentPane.add(sp, BorderLayout.CENTER);

		serialHandler = new SerialComm();
		setVisible(true);
	}

	/**
	 * Updates the ports combobox with the string ID's of the available serial ports
	 */
	public void updateCommPortComboBox() {
		ArrayList<String> commPortIDList = serialHandler.findPorts();
		if (commPortIDList != null) {
			commPortCombobox.setEnabled(true);
			commPortCombobox.setModel(new DefaultComboBoxModel(commPortIDList.toArray()));
		} else {
			generalStatusLabel.setText("No Serial Dongle Found");
		}

	}

	/**
	 * This method handles which methods will be called when the user selects a port
	 * from the COMM port combobox. This entails looking up which port they selected
	 * and then opening that port
	 */
	private void portSelectedHandler() {
		disconnectButton.setEnabled(true);
		try {
			// Executes if the user selected a valid COMM port
			if (commPortCombobox.getSelectedItem() != null) {

				// Get the string identifier (name) of the port the user selected
				String selectedCommID = commPortCombobox.getSelectedItem().toString();

				// Open the serial port with the selected name, initialize input and output
				// streams, set necessary flags so the whole program know that everything is
				// initialized
				if (serialHandler.openSerialPort(selectedCommID)) {

					// Notify the user that the port as opened successfully and is ready for a new
					// command
					generalStatusLabel.setText("Serial Port Opened Successfully, Awaiting Commands");
					readDebugData();
				}
			}
		} catch (IOException e) {
			generalStatusLabel.setText("Error Communicating With Serial Dongle");
		} catch (PortInUseException e) {
			generalStatusLabel.setText("Serial Port Already In Use");
		}
	}

	/**
	 * Executed when disconnect button is pressed. Since this is an action event, it
	 * must complete before GUI changes will be visible
	 */
	public void disconnectButtonHandler() {
		serialHandler.closeSerialPort();

		// Notify the user that the port has been closed
		generalStatusLabel.setText("Port Closed");

		// Disable buttons that only work when the port is opened
		disconnectButton.setEnabled(false);

		// Re-enable COMM port combobox so the user can select a new port to connect to
		commPortCombobox.setEnabled(true);
	}

	public void readDebugData() throws IOException {
		data.clear();
		formattedData.clear();
		
		Runnable readData = new Runnable() {
			public void run() {
				boolean reading = false;
				int counter = 0;
				while (true) {

					try {
						//System.out.println(serialHandler.getInputStream().available());
						if (serialHandler.getInputStream().available() > 0) {
							int temp = serialHandler.getInputStream().read();
							//System.out.println(temp);
							if (temp == 12) {
								dataDsp.setText("");
								reading = !reading;
							}
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (reading) {

						int[] measurement=null;
						try {
							
							measurement = serialHandler.readData();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (measurement == null) {
							reading = !reading;
							try {
								int temp = serialHandler.getInputStream().read();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
						} else {
							data.add(formatDataString(measurement, counter));
							counter++;
						}
					}
				}
				fillTextArea();
			}
		};
		
		
		Thread readDataThread = new Thread(readData);
		readDataThread.start();
		
	}

	
	public String formatDataString(int[] sample, int sampleTime) {
		String sampleStr = ""; 
		
		sampleStr = addZeros(sample[4])  + "  ";
		sampleStr += addZeros(sample[0])  + "  "; 
		sampleStr += addZeros(sample[1])  + "  ";
		sampleStr += addZeros(sample[3])  + "  ";
		sampleStr += addZeros(sample[2])  + "  "; 
		sampleStr += addZeros(sampleTime) + "  \t\t";
		
		return sampleStr;
	}
	
	public String addZeros(int sample) {
		String SampleStr = "";
		
		if(sample < 10) {
			SampleStr = "  " + Integer.toString(sample);
		} else if(sample < 100) {
			SampleStr = " " + Integer.toString(sample);
		} else {
			SampleStr = Integer.toString(sample);
		}
		
		return SampleStr;
	}
	
	public void formateTextArea(int numRows) {
		int appendNum = data.size() / numRows;
		int extraAppends = data.size() % numRows;
		
		String dataLabels = "RAW  BL   STD  JIT  SPC  INT  \t\t";
		
		for(int i = 0; i <= appendNum; i++) {
			dataDsp.append(dataLabels);
	    }
		dataDsp.append("\n");		
		
		
		for(int i = 0; i < numRows; i++) {
			formattedData.add(data.get(i));	
			for(int j = 1; j < appendNum; j++) {
				formattedData.set(i, formattedData.get(i) + (data.get((j  * numRows) + i)));
			}
		}
		
		for(int i = 0; i < extraAppends; i++) {
			formattedData.set(i, formattedData.get(i) + (data.get(i + appendNum*numRows)));
		}
		
		for(int i = 0; i < numRows; i++) {
			formattedData.set(i, formattedData.get(i) + "\n");
		}
		
	}
	
	
	public void fillTextArea() {
		
		int numRows = (int) sp.getBounds().getHeight() / 21;
		if(numRows>data.size()) {
			numRows=data.size();
		}
		formateTextArea(numRows);
		
		for (int i = 0; i < formattedData.size(); i++) {
				dataDsp.append(formattedData.get(i));
		}
	
		try {
			readDebugData();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
