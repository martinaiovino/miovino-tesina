package project;

import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Labeled;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class MainGUI extends Application {
	private static Pane upload_pane = new Pane();
	private static TextArea txtArea = new TextArea();	
	private Desktop desktop = Desktop.getDesktop();
	String datasetPath = "";
	String configPath = "";
	
	@Override
	public void start(Stage stage) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("mainGUI.fxml"));
			
			Scene scene = new Scene(root, 632, 602);
		    
	        stage.setTitle("DQ Evaluator");
	        stage.setScene(scene);
	        stage.show();
	        
	        RadioButton radio_existingdataset = (RadioButton) scene.lookup("#rb_search");
	        RadioButton radio_importdataset = (RadioButton) scene.lookup("#rb_upload");
	        final ToggleGroup group = new ToggleGroup();
	        radio_existingdataset.setToggleGroup(group);
	        radio_importdataset.setToggleGroup(group);
	        
	        upload_pane = (Pane) scene.lookup("#upload_pane");
	        txtArea = (TextArea) scene.lookup("#text_area");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
	
	@FXML protected void radioExistingSelected(ActionEvent event) throws IOException {
		upload_pane.setVisible(false);
		txtArea.setVisible(false);
    }
	
	@FXML protected void radioUploadSelected(ActionEvent event) {
		upload_pane.setVisible(true);
		txtArea.setVisible(false);
    }
	
	@FXML protected void uploadDatasetBtn(ActionEvent event) {
		datasetPath = selectFile(event, "selected_dataset");
		checkDatasetConfig(event);
    }
	
	@FXML protected void uploadConfigBtn(ActionEvent event) {
		configPath = selectFile(event, "selected_config");
		checkDatasetConfig(event);
		txtArea.setText("");
		txtArea.setVisible(false);
    }
	
	@FXML protected void openSelectedDaset(ActionEvent event) throws IOException {
		File file = new File(datasetPath);
		desktop.open(file);
    }
	
	@FXML protected void openSelectedConfig(ActionEvent event) throws IOException {
		File file = new File(configPath);
		desktop.open(file);
    }
	
	@SuppressWarnings("unchecked")
	@FXML protected void clickGenerateConfig(ActionEvent event) {
		JSONArray datasetFile = new JSONArray();
		
		Node node = (Node) event.getSource();
        ((Hyperlink) node.getScene().lookup("#selected_config")).setText("");
        node.getScene().lookup("#save_btn").setVisible(true);

		try
        {
			//read the dataset file
        	File f = new File(datasetPath); // look for the file
        	JSONParser jsonParser = new JSONParser(); // parser
        	FileReader reader = new FileReader(f.getAbsolutePath());
        	datasetFile = (JSONArray) jsonParser.parse(reader);
        	
        	//start building the content of the derived configuration file
        	JSONObject configFile = new JSONObject(); //config file
        	
        	JSONObject dt = DatasetToConfig.buildDataTypes(datasetFile); //datatypes
        	configFile.put("datatypes", dt);
        	
        	JSONObject vr = DatasetToConfig.buildValuesRange(dt, datasetFile); //values range
        	configFile.put("values_range", vr);
        	
        	if (DatasetToConfig.isStream) { //source type
        		configFile.put("source_type", "stream");
        	} else {
        		configFile.put("source_type", "batch");
        	}
        	
          	configFile.put("volatility", 350000000); //volatility
          	
          	Gson gson = new GsonBuilder().setPrettyPrinting().create();
          	JsonParser jp = new JsonParser();
          	JsonElement je = jp.parse(configFile.toString());
          	String prettyJsonString = gson.toJson(je);
          	
          	//Node node = (Node) event.getSource();
          	//TextArea txtArea = (TextArea) node.getScene().lookup("#text_area");
          	txtArea.setVisible(true);
        	txtArea.setText(prettyJsonString);
        	//DatasetToConfig.saveFile(configFile.toJSONString()); //ask to save file
        } catch (Exception e) { //FileNotFoundException
            e.printStackTrace();
        }
	}
	
	protected String selectFile(ActionEvent event, String hyperId) {
		String returnPath = "";
		Node node = (Node) event.getSource();
        final FileChooser fileChooser = new FileChooser();
        String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
        fileChooser.setInitialDirectory(new File(currentPath + "/resources"));
		File file = fileChooser.showOpenDialog(node.getScene().getWindow());
        if (file != null) {
            Hyperlink selectedDataset = (Hyperlink) node.getScene().lookup("#" + hyperId);
            selectedDataset.setVisible(true);
            returnPath = file.getPath();
            selectedDataset.setText(file.getName());
        }
        return returnPath;
	}
	
	protected void checkDatasetConfig(ActionEvent event) {
		Node node = (Node) event.getSource();
		if (datasetPath != "" && configPath != "") {
			node.getScene().lookup("#evalaluate_btn").setVisible(true);
		} else {
			node.getScene().lookup("#evalaluate_btn").setVisible(false);
		}
	}
	
	@FXML protected void saveBtn (ActionEvent event) throws IOException {
		Node node = (Node) event.getSource();
        final FileChooser fileChooser = new FileChooser();
        String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
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
            
            node.getScene().lookup("#evalaluate_btn").setVisible(true);
            node.getScene().lookup("#save_btn").setVisible(false);
            txtArea.setVisible(false);
        }
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
					JSONFile.calculateStreamMetrics(configJSONObject, datasetJSONArray);
					result = JSONFile.buildDQEvaluator("stream");
				}
			}
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
          	JsonParser jp = new JsonParser();
          	JsonElement je = jp.parse(result.toString());
          	String prettyJsonString = gson.toJson(je);
          	
          	//Node node = (Node) event.getSource();
          	//TextArea txtArea = (TextArea) node.getScene().lookup("#text_area");
          	txtArea.setVisible(true);
        	txtArea.setText(prettyJsonString);
        	Node node = (Node) event.getSource();
        	node.getScene().lookup("#save_btn").setVisible(true);
			//System.out.println(result);
		} else {
			System.out.println("Files not found");
		}
	}
}
