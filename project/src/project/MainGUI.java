package project;

import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class MainGUI extends Application {
	private static TextArea txtArea = new TextArea();
	private static VBox container = new VBox();
	private static ComboBox<String> dateformat = new ComboBox<String>();
	private Desktop desktop = Desktop.getDesktop();
	private static String datasetPath = "";
	private static String configPath = "";
	private static String configBackup = "";
	private static String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
    private static String selectedDateformat = "yyyy-MM-dd";
	
	@SuppressWarnings("unchecked")
	@Override
	public void start(Stage stage) {
		try {
			// Get FXML file
			Parent root = FXMLLoader.load(getClass().getResource("MainGUI2.fxml"));
			
			// Size and title of the window
			Scene scene = new Scene(root, 1196, 642);
	        stage.setTitle("DQ Evaluator");
	        stage.setScene(scene);
	        stage.show(); 
	        stage.setMaxWidth(1220);
	        stage.setMaxHeight(680);
	        
	        // Radio buttons - options to search or upload a dataset
	        RadioButton radio_existingdataset = (RadioButton) scene.lookup("#rb_search");
	        RadioButton radio_importdataset = (RadioButton) scene.lookup("#rb_upload");
	        final ToggleGroup group = new ToggleGroup();
	        radio_existingdataset.setToggleGroup(group);
	        radio_importdataset.setToggleGroup(group);
	        
	        txtArea = (TextArea) scene.lookup("#text_area");
	        container = (VBox) scene.lookup("#panecheckbox");
	        // Add values to Combobox
	        dateformat = (ComboBox<String>) scene.lookup("#dateformat");
	        dateformat.getItems().addAll(
	        	    "yyyy-MM-dd",
	        	    "dd-MM-YYYY",
	        	    "MM-dd-YYYY",
	        	    "yyyy-MM-dd HH:mm",
	        	    "yyyy-MM-dd HH:mm:ss",
	        	    "yyyy/MM/dd",
	        	    "dd/MM/YYYY",
	        	    "MM/dd/YYYY",
	        	    "yyyy/MM/dd HH:mm",
	        	    "yyyy/MM/dd HH:mm:ss"
	        	);
	        dateformat.getSelectionModel().select(0);
	        
	        dateformat.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal)->{
	            if(newVal != null) { //if a new date format is selected, need to generate the configuration file again
	            	//restartHidePanes(scene.getFocusOwner());
	            	selectedDateformat = (String) newVal;
	            	//System.out.println("!!" + selectedDateformat);
	            }
	        });
	        	        
	        ComboBox<String> existingDatasets = (ComboBox<String>) scene.lookup("#existing_datasets");
	        existingDatasets.setPromptText("Select a dataset...");
	        File dir = new File(currentPath + "/resources");
	        File[] directoryListing = dir.listFiles();
	        if (directoryListing != null) {
	          for (File child : directoryListing) {
	        	  if (child.getName().contains(".json")) {
	        		  existingDatasets.getItems().add(child.getName());
	        	  }
	          }
	        }
	        existingDatasets.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal)->{
	            if(newVal != null) {
	            	Hyperlink hyperselected = (Hyperlink) scene.lookup("#selected_dataset");
	                hyperselected.setText((String) newVal);
	                hyperselected.setVisible(true);
	                datasetPath = currentPath + "/resources/" + newVal;
	                
	                String filex = newVal.toString();
	                Hyperlink hyperselectedconf = (Hyperlink) scene.lookup("#selected_config");
	                hyperselectedconf.setText(filex.substring(0, filex.length() - 5) + "_config.json");
	                hyperselectedconf.setVisible(true);
	                configPath = currentPath + "/resources/configurationFiles/" + filex.substring(0, filex.length() - 5) + "_config.json";
	                checkDatasetConfig(scene.getFocusOwner());
	            }
	        });
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
	
	// Show or hide panes depending on the selected option
	@FXML protected void radioExistingSelected(ActionEvent event) throws IOException {
		Node node = (Node) event.getSource();
		selectedOption(node, true);
    }
	
	@FXML protected void radioUploadSelected(ActionEvent event) {
		Node node = (Node) event.getSource();
		selectedOption(node, false);
    }
	
	protected void selectedOption(Node node, Boolean isExisting) {
		node.getScene().lookup("#existing_datasets").setVisible(isExisting);
		node.getScene().lookup("#upload_pane").setVisible(true);
		node.getScene().lookup("#upload_dataset").setDisable(isExisting);
		node.getScene().lookup("#upload_config").setDisable(isExisting);
		node.getScene().lookup("#generate_config").setDisable(true);
		((Hyperlink) node.getScene().lookup("#selected_dataset")).setText("");
		((Hyperlink) node.getScene().lookup("#selected_config")).setText("");
		((ComboBox) node.getScene().lookup("#existing_datasets")).setValue(null);
		datasetPath = "";
		configPath = "";
		restartHidePanes(node);
	}
	
	//Upload dataset or configuration file: need to open the folders to pick a file
	@FXML protected void uploadDatasetBtn(ActionEvent event) {
		Node node = (Node) event.getSource();
		datasetPath = selectFile(event, "selected_dataset");
		checkDatasetConfig(node);
    }
	
	@FXML protected void uploadConfigBtn(ActionEvent event) {
		Node node = (Node) event.getSource();
		configPath = selectFile(event, "selected_config");
		checkDatasetConfig(node);
		txtArea.setText("No data to display.");
		txtArea.setDisable(true);
    }
	
	// Open the selected files (hyperlink)
	@FXML protected void openSelectedDaset(ActionEvent event) throws IOException {
		File file = new File(datasetPath);
		desktop.open(file);
    }
	
	@FXML protected void openSelectedConfig(ActionEvent event) throws IOException {
		File file = new File(configPath);
		desktop.open(file);
    }
	
	// Method for generating the configuration file
	@SuppressWarnings("unchecked")
	@FXML protected void clickGenerateConfig(ActionEvent event) {
		JSONArray datasetFile = new JSONArray();
		Node node = (Node) event.getSource();
		configPath = "";
        ((Hyperlink) node.getScene().lookup("#selected_config")).setText(configPath);

		try
        {
			//read the dataset file
        	File f = new File(datasetPath); // look for the file
        	JSONParser jsonParser = new JSONParser(); // parser
        	FileReader reader = new FileReader(f.getAbsolutePath());
        	datasetFile = (JSONArray) jsonParser.parse(reader);
        	
        	//start building the content of the derived configuration file
        	JSONObject configFile = new JSONObject(); //config file
        	
        	JSONObject dt = DatasetToConfig.buildDataTypes(datasetFile, selectedDateformat); //datatypes
        	configFile.put("datatypes", dt);
        	
        	JSONObject vr = DatasetToConfig.buildValuesRange(dt, datasetFile); //values range
        	configFile.put("values_range", vr);
        	
        	if (DatasetToConfig.isStream) { //source type
        		configFile.put("source_type", "stream");
        	} else {
        		configFile.put("source_type", "batch");
        	}
        	
          	configFile.put("volatility", 350000000); //volatility
          	
          	//json beautifier
          	Gson gson = new GsonBuilder().setPrettyPrinting().create();
          	JsonParser jp = new JsonParser();
          	JsonElement je = jp.parse(configFile.toString());
          	configBackup = gson.toJson(je); //configuration file backup in case it will be filtered
          	txtArea.setDisable(false);
        	txtArea.setText(configBackup);
            
        	//display as many checkboxes as the datatypes found in the dataset
        	Label filtersText = new Label("FILTERS:");
        	filtersText.setStyle("-fx-font-weight: bold; -fx-padding: 2 2 2 5;");
        	List<CheckBox> checkBoxes = new ArrayList<>();
            node.getScene().lookup("#scrollpanecheckbox").setVisible(true);
            dt.keySet().forEach(keyStr ->
            {
                CheckBox c = new CheckBox((String) keyStr); 
            	c.setSelected(true);
            	c.setStyle("-fx-padding: 2 2 2 5;");
                checkBoxes.add(c);
            });
            container.getChildren().clear();
            container.getChildren().addAll(filtersText);
            container.getChildren().addAll(checkBoxes);
            node.getScene().lookup("#apply_btn").setVisible(true);
            node.getScene().lookup("#evaluate_btn").setVisible(false);
            node.getScene().lookup("#save_btn").setVisible(true);
        } catch (Exception e) { //FileNotFoundException
            e.printStackTrace();
        }
	}
	
	// Method for File chooser
	protected String selectFile(ActionEvent event, String hyperId) {
		String returnPath = "";
		Node node = (Node) event.getSource();
        final FileChooser fileChooser = new FileChooser();
        String addToPath = (hyperId == "selected_config" ? "/configurationFiles" : "");
        fileChooser.setInitialDirectory(new File(currentPath + "/resources" + addToPath));
		File file = fileChooser.showOpenDialog(node.getScene().getWindow());
        if (file != null) {
            Hyperlink selectedDataset = (Hyperlink) node.getScene().lookup("#" + hyperId);
            selectedDataset.setVisible(true);
            returnPath = file.getPath();
            selectedDataset.setText(file.getName());
            
            if (addToPath == "") { //upload dataset file selected
            	node.getScene().lookup("#generate_config").setDisable(false);
            	restartHidePanes(node);
            }
        }
        return returnPath;
	}
	
	protected void restartHidePanes(Node node) {
		//node.getScene().lookup("#evalaluate_btn").setVisible(false);
    	node.getScene().lookup("#save_btn").setVisible(false);
    	node.getScene().lookup("#apply_btn").setVisible(false);
    	node.getScene().lookup("#scrollpanecheckbox").setVisible(false);
    	txtArea.setText("No data to display.");
    	txtArea.setDisable(true);
    	checkDatasetConfig(node);
	}
	
	
	// Hide or show evaluate button
	protected void checkDatasetConfig(Node node) {
		if (datasetPath != "" && datasetPath != null && configPath != "" && configPath != null) {
			node.getScene().lookup("#evaluate_btn").setVisible(true);
		} else {
			node.getScene().lookup("#evaluate_btn").setVisible(false);
		}
	}
	
	@FXML protected void saveBtn (ActionEvent event) throws IOException {
		Node node = (Node) event.getSource();
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(currentPath + "/resources"));
        
        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON file (*.json)", "*.json");
        fileChooser.getExtensionFilters().add(extFilter);
        
		File file = fileChooser.showSaveDialog(node.getScene().getWindow());
        if (file != null) {
        	FileWriter fileWriter = null;
            
            fileWriter = new FileWriter(file);
            fileWriter.write(txtArea.getText());
            fileWriter.close();
            
            Hyperlink hyperselected = (Hyperlink) node.getScene().lookup("#selected_config");
            hyperselected.setText(file.getName());
            hyperselected.setVisible(true);
            configPath = file.getPath();
            
            node.getScene().lookup("#evaluate_btn").setVisible(true);
            node.getScene().lookup("#save_btn").setVisible(false);
            //txtArea.setVisible(false);
            node.getScene().lookup("#scrollpanecheckbox").setVisible(false);
            node.getScene().lookup("#apply_btn").setVisible(false);
        }
	}
	
	@FXML protected void applyBtn (ActionEvent event) throws IOException, ParseException {
		String config = configBackup;
		JSONParser parser = new JSONParser(); 
		JSONObject editedConfigFile = (JSONObject) parser.parse(config);
		for (Object node : container.getChildren()) {
			if (node instanceof CheckBox) {
				if (!((CheckBox) node).isSelected()) {
					editedConfigFile = removeElement(editedConfigFile, ((CheckBox) node).getText());
				}
			}
		}     
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
      	JsonParser jp = new JsonParser();
      	JsonElement je = jp.parse(editedConfigFile.toString());
      	String prettyJsonString = gson.toJson(je);
    	txtArea.setText(prettyJsonString);
	}
	
	private static JSONObject removeElement(JSONObject editedConfigFile, String key) {
		JSONObject valrange = (JSONObject) editedConfigFile.get("values_range");
		valrange.put(key, editedConfigFile.remove(key));
		JSONObject datatypes = (JSONObject) editedConfigFile.get("datatypes");
		datatypes.put(key, editedConfigFile.remove(key));
		return editedConfigFile;
	}
	
	@FXML protected void evaluateDataset (ActionEvent event) {		
		// Read the dataset and configuration files
		Object returnedDataset = JSONFile.readFile(datasetPath); // dataset object
		Object returnedConfig = JSONFile.readFile(configPath); //configuration file object
		String result = "Unknown data type";
		
		//Check whether the two objects are not null
		if (returnedDataset != null && returnedConfig != null) {
			JSONArray datasetJSONArray = (JSONArray) returnedDataset; // dataset JSON Array
			System.out.println("Total number of data: " + datasetJSONArray.size() + "\n"); // total number of data
			
			JSONObject configJSONObject = (JSONObject) returnedConfig; // configuration JSON Object
			
			JSONFile.prepare_consistency(configJSONObject, datasetJSONArray);
			
			//Check the source type
			String sourceType = (String) configJSONObject.get("source_type");
			//Source type can be either batch or stream
			if (sourceType.equals("batch")) {
				JSONFile.calculateBatchMetrics(configJSONObject, datasetJSONArray); 
				result = JSONFile.buildDQEvaluator("batch");
			} else {
				if (sourceType.equals("stream")) {
					JSONFile.calculateStreamMetrics(configJSONObject, datasetJSONArray, selectedDateformat);
					result = JSONFile.buildDQEvaluator("stream");
				}
			}
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
          	JsonParser jp = new JsonParser();
          	JsonElement je = jp.parse(result.toString());
          	String prettyJsonString = gson.toJson(je);
          	
          	//Node node = (Node) event.getSource();
          	//TextArea txtArea = (TextArea) node.getScene().lookup("#text_area");
          	txtArea.setDisable(false);
        	txtArea.setText(prettyJsonString);
        	Node node = (Node) event.getSource();
        	node.getScene().lookup("#save_btn").setVisible(true);
			//System.out.println(result);
		} else {
			System.out.println("Files not found");
		}
	}
}
