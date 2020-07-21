package project;

import java.awt.EventQueue;

import javax.swing.JFrame;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.swing.JButton;
//import java.awt.FlowLayout;
import javax.swing.JComboBox;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.TextArea;

public class MainFrame {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainFrame window = new MainFrame();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainFrame() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 685, 410);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JButton btnNewButton = new JButton("Analyse");
		btnNewButton.setFont(new Font("Calibri", Font.PLAIN, 16));
		btnNewButton.setBackground(Color.LIGHT_GRAY);
		btnNewButton.setBounds(250, 15, 115, 29);
		frame.getContentPane().add(btnNewButton);
		
		//get the available files
		String[] pathnames;
		File f = new File("resources");
		pathnames = f.list();
		ArrayList<String> availableFiles = new ArrayList<>();
		for (String pathname : pathnames) { 
			if(!pathname.endsWith("_config.json")) {
				availableFiles.add(pathname.replace(".json", ""));
			}
		}
		
		JComboBox comboBox = new JComboBox(availableFiles.toArray());
		comboBox.setFont(new Font("Calibri", Font.PLAIN, 16));
		comboBox.setForeground(new Color(0, 0, 0));
		comboBox.setBackground(new Color(255, 255, 255));
		comboBox.setBounds(15, 16, 207, 26);
		frame.getContentPane().add(comboBox);
		
		TextArea textArea = new TextArea();
		textArea.setFont(new Font("Calibri", Font.PLAIN, 12));
		textArea.setEditable(false);
		textArea.setBounds(15, 71, 638, 273);
		frame.getContentPane().add(textArea);
				
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String selectedOption = (String) comboBox.getSelectedItem().toString();
								
				// Read the dataset and configuration files
				Object returnedDataset = JSONFile.readFile(selectedOption + ".json"); // dataset object
				Object returnedConfig = JSONFile.readFile(selectedOption + "_config.json"); //configuration file object
				
				//Check whether the two objects are not null
				if (returnedDataset != null && returnedConfig != null) {
					JSONArray datasetJSONArray = (JSONArray) returnedDataset; // dataset JSON Array
					System.out.println("Total number of data: " + datasetJSONArray.size() + "\n"); // total number of data
					
					JSONObject configJSONObject = (JSONObject) returnedConfig; // configuration JSON Object
					
					//Check the source type
					String sourceType = (String) configJSONObject.get("source_type");
					String result = "Unknown data type";
					if (sourceType.equals("batch")) {
						JSONFile.calculateBatchMetrics(configJSONObject, datasetJSONArray);
						result = JSONFile.buildDQEvaluator("batch");
					} else {
						if (sourceType.equals("stream")) {
							JSONFile.calculateStreamMetrics(configJSONObject, datasetJSONArray);
							result = JSONFile.buildDQEvaluator("stream");
						}
					}
					textArea.setText(result);
				} else {
					System.out.println("Files not found");
				}
			}
		});
	}
}

