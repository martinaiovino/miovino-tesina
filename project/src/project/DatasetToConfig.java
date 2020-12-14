package project;

import java.io.File;

import java.text.SimpleDateFormat;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class DatasetToConfig {
	
	static Boolean isStream = false; //boolean for the source type

	// Method to build the datatypes object
	public static JSONObject buildDataTypes (JSONArray datasetJSONArray, String dateformat) {
		JSONObject datatypes = new JSONObject();
		String tmp = "";
		for (int i = 0; i < datasetJSONArray.size(); i++) { //iterate inside the dataset file
			JSONObject datasetObji = (JSONObject) datasetJSONArray.get(i);
			for (Object v : datasetObji.keySet()) { //for each value of the dataset file
				if (datasetObji.get(v) instanceof JSONObject)  {
					tmp += v;
					JSONObject datasetObj2 = (JSONObject) datasetObji.get(v);
					for (Object k : datasetObj2.keySet()) {
						getDataType(datasetObj2, datatypes, k, tmp, dateformat);
					}
				} else {
					getDataType(datasetObji, datatypes, v, tmp, dateformat);
			}
				tmp = "";
			}
		}
		return datatypes;
	}
	
	@SuppressWarnings("unchecked")
	public static void getDataType(JSONObject datasetObji, JSONObject datatypes, Object v, String tmp, String dateformat) {
		try {
			//take the i-th element and check its type. Then, add it to the datatypes object
			String subval = datasetObji.get(v).toString();
			String val = (tmp=="" ? v.toString() : tmp+"#"+v);
			if (isNumeric(subval)) { //check if it is a number
				datatypes.put(val, "float");
			} else {
				if (isTimestamp(subval, dateformat)) { //check if it is a timestamp
					datatypes.put(val, "timestamp");
					isStream = true;
				} else { //if it is not a number nor timestamp, it is a string
					datatypes.put(val, "categ");
				}
			}
			} catch (Exception e) {
				System.out.println("EXC" + datasetObji + " " + tmp);
			}
	}
	
	
	// Method to build the values_range object
	@SuppressWarnings("unchecked")
	public static JSONObject buildValuesRange (JSONObject datatypes, JSONArray datasetJSONArray) {
		JSONObject extern = new JSONObject();
		
		for (Object v : datatypes.keySet()) { //iterate inside the datatypes Object
			String subval = (String) datatypes.get(v); //v -> key; subval -> value
			JSONObject minmax = new JSONObject();
			JSONObject interval = new JSONObject();
			switch(subval) { //check whether the type of the value of the i-th key
			case "timestamp": 
				//if it is timestamp, it does not have to be added to the values_range object
				break;
			case "float":
				//if it is a float, build the min-max interval object
				double[] arrayMinMax = getMinMax((String) v, datasetJSONArray);
				minmax.put("min", arrayMinMax[0]);
				minmax.put("max", arrayMinMax[1]);
				interval.put("interval", minmax);
				extern.put(v.toString(), interval);
				break;
			case "categ":
				//if it is a float, build the array interval object
				JSONArray arrayVal = getArray((String) v, datasetJSONArray);
				interval.put("interval", arrayVal);
				extern.put(v.toString(), interval);
				break;
			}
		}
		
		return extern;
	}
	
	// Method to check if a value is numeric
	public static boolean isNumeric(String strNum) {
	    if (strNum == null) {
	        return false;
	    }
	    try {
	    	@SuppressWarnings("unused")
	        double d = Double.parseDouble(strNum);
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	    return true;
	}
	
	// Method to check if a value is a timestamp
	public static boolean isTimestamp(String strDate, String dateFormat) {
	    if (strDate == null) {
	        return false;
	    }
	    try {
	        @SuppressWarnings("unused")
			Date d = new SimpleDateFormat(dateFormat).parse(strDate);
	    } catch (Exception nfe) {
	        return false;
	    }
	    return true;
	}
	
	// Method to get the array interval of a values_range object element
	@SuppressWarnings("unchecked")
	public static JSONArray getArray (String key, JSONArray dataset) {
		String[] array = new String[dataset.size()]; //define an array
		for (int i = 0; i < dataset.size(); i++) { //iterate inside the dataset
			try {
				JSONObject datasetObji = (JSONObject) dataset.get(i); //get the i-th object of the array of objects
				if (key.contains("#")) {
					String[] keyobj = key.split("#");
					datasetObji = (JSONObject) datasetObji.get(keyobj[0]);
					array[i] = datasetObji.get(keyobj[1]).toString(); //get the value of the searched key and add it to the array
				} else {
					array[i] = datasetObji.get(key).toString(); //get the value of the searched key and add it to the array
				}
			} catch (Exception e) {
				
			}
		}
		//only take the distinct values of the array
		String[] unique = Arrays.stream(array).distinct().toArray(String[]::new);
		JSONArray toReturn = new JSONArray();
		for (String elem : unique) {
			if (elem != null) {
				toReturn.add(elem);
			}
		}
		return toReturn;
	}
	
	// Method to get the min-max interval of a values_range object element
	public static double[] getMinMax (String key, JSONArray dataset) {
		//get the first value (given the key) of the first object of dataset (array of objects)
		JSONObject datasetObji = (JSONObject) dataset.get(0);
		double [] minmax = new double [2];
		Boolean ismin = false;
		Boolean ismax = false;
		//iterate inside the dataset (skipping the first object already analized)
		for (int i = 0; i < dataset.size(); i++) {
			try {
			double thisDouble = 0;
			datasetObji = (JSONObject) dataset.get(i);
			if (key.contains("#")) {
				String[] keyobj = key.split("#");
				datasetObji = (JSONObject) datasetObji.get(keyobj[0]);
				thisDouble = Double.parseDouble(datasetObji.get(keyobj[1]).toString());
			} else {
			thisDouble = Double.parseDouble(datasetObji.get(key).toString());
			}
			//if the value of the i-th object is < min, assign min to this value
			if (!ismin || minmax[0] > thisDouble) {
				minmax[0] = thisDouble;
				ismin = true;
			} //if the value of the i-th object is > max, assign to max this value
			if (!ismax || minmax[1] < thisDouble) {
				minmax[1] = thisDouble;
				ismax = true;
			}
			} catch (Exception e) {
				//case null value
			}
		}
		return minmax;
	}
	
	/* The following code is NOT executed in case the GUI is used */
	// Method to save the generated configuration file
	public static void saveFile (String jsonContent) {
		@SuppressWarnings("resource")
		Scanner keyboard = new Scanner(System.in);
    	System.out.println("Do you want to save the generated config file? Press y to accept, press any other key to exit");
    	String response = keyboard.next();
    	if (response.equals("y")) {
    	try {
    		System.out.println("Type the file name: ");
        	String filen = keyboard.next();
            FileWriter file = new FileWriter("resources/" + filen + ".json");
            file.write(jsonContent);
            file.flush();
            file.close();
            System.out.println("File saved! Refresh the resources folder");
        } catch (IOException e) {
            e.printStackTrace();
        }
    	} else {
    		System.out.println("Bye");
    	}
	}
	
	@SuppressWarnings("unchecked")
	public static void main (String[] args) {
		String fileName = "collections.json"; // File name
		JSONArray datasetFile = new JSONArray();
		
		try
        {
			//read the dataset file
        	File f = new File("resources/" + fileName); // look for the file
        	JSONParser jsonParser = new JSONParser(); // parser
        	FileReader reader = new FileReader(f.getAbsolutePath());
        	datasetFile = (JSONArray) jsonParser.parse(reader);
        	
        	//start building the content of the derived configuration file
        	JSONObject configFile = new JSONObject(); //config file
        	
        	String dateFormat = "yyyy-MM-dd"; // date format here 
        	JSONObject dt = buildDataTypes(datasetFile, dateFormat); //datatypes
        	configFile.put("datatypes", dt);
        	
        	JSONObject vr = buildValuesRange(dt, datasetFile); //values range
        	configFile.put("values_range", vr);
        	
        	if (isStream) { //source type
        		configFile.put("source_type", "stream");
        	} else {
        		configFile.put("source_type", "batch");
        	}
        	
          	configFile.put("volatility", 350000000); //volatility
          	          	
          	//print the configuration file as a string
          	Gson gson = new GsonBuilder().setPrettyPrinting().create();
          	JsonParser jp = new JsonParser();
          	JsonElement je = jp.parse(configFile.toString());
          	String prettyJsonString = gson.toJson(je);
        	System.out.println(prettyJsonString);
        	saveFile(prettyJsonString); //ask to save file
        } catch (Exception e) { //FileNotFoundException
            e.printStackTrace();
        }
	}
}
