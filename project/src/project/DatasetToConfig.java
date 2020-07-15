package project;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class DatasetToConfig {
	
	static Boolean isStream = false; //boolean for the source type
	
	@SuppressWarnings("unchecked")
	public static void main (String[] args) {
		String fileName = "getNutritionalData.json"; // File name
		JSONArray datasetFile = new JSONArray();
		
		try
        {
			//read the dataset file
        	File f = new File("resources/" + fileName); // look for the file
        	System.out.println(f.getAbsolutePath());
        	JSONParser jsonParser = new JSONParser(); // parser
        	FileReader reader = new FileReader(f.getAbsolutePath());
        	datasetFile = (JSONArray) jsonParser.parse(reader);
        	
        	//start building the content of the derived configuration file
        	JSONObject configFile = new JSONObject(); //config file
        	
        	JSONObject dt = buildDataTypes(datasetFile); //datatypes
        	configFile.put("datatypes", dt);
        	
        	JSONObject vr = buildValuesRange(dt, datasetFile); //values range
        	configFile.put("values_range", vr);
        	
        	if (isStream) { //source type
        		configFile.put("source_type", "stream");
        	} else {
        		configFile.put("source_type", "batch");
        	}
        	
          	configFile.put("volatility", 350000000); //volatility
          	
          	//TODO association rules
          	
          	//print the configuration file as a string
        	System.out.println(configFile.toJSONString());
        	saveFile(configFile.toJSONString()); //ask to save file
        } catch (Exception e) { //FileNotFoundException
            e.printStackTrace();
        }
	}
	
	// Method to build the datatypes object
	@SuppressWarnings("unchecked")
	public static JSONObject buildDataTypes (JSONArray datasetJSONArray) {
		JSONObject datatypes = new JSONObject();
		for (int i = 0; i < datasetJSONArray.size(); i++) { //iterate inside the dataset file
			JSONObject datasetObji = (JSONObject) datasetJSONArray.get(i);
			for (Object v : datasetObji.keySet()) { //for each value of the dataset file
				try {
				//take the i-th element and check its type. Then, add it to the datatypes object
				String subval = datasetObji.get(v).toString();
				if (isNumeric(subval)) { //check if it is a number
					datatypes.put(v, "float");
				} else {
					if (isTimestamp(subval)) { //check if it is a timestamp
						datatypes.put(v, "timestamp");
						isStream = true;
					} else { //if it is not a number nor timestamp, it is a string
						datatypes.put(v, "categ");
					}
				}
				} catch (Exception e) {
					
				}
			}
		}
		return datatypes;
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
				minmax.put("min", Math. round(arrayMinMax[0] * 10) / 10.0);
				minmax.put("max", Math. round(arrayMinMax[1] * 10) / 10.0);
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
	public static boolean isTimestamp(String strDate) {
	    if (strDate == null) {
	        return false;
	    }
	    try {
	        @SuppressWarnings("unused")
			Date d = new SimpleDateFormat("yyyy-MM-dd").parse(strDate);
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
			JSONObject datasetObji = (JSONObject) dataset.get(i); //get the i-th object of the array of objects
			array[i] = datasetObji.get(key).toString(); //get the value of the searched key and add it to the array
		}
		//only take the distinct values of the array
		String[] unique = Arrays.stream(array).distinct().toArray(String[]::new);
		JSONArray toReturn = new JSONArray();
		for (String elem : unique) {
			toReturn.add(elem);
		}
		return toReturn;
	}
	
	// Method to get the min-max interval of a values_range object element
	public static double[] getMinMax (String key, JSONArray dataset) {
		//get the first value (given the key) of the first object of dataset (array of objects)
		JSONObject datasetObji = (JSONObject) dataset.get(0);
		//the first value is firstly set as min and max
		double [] minmax = new double [2];
		minmax[0] = Double.parseDouble(datasetObji.get(key).toString());
		minmax[1] = Double.parseDouble(datasetObji.get(key).toString());
		//iterate inside the dataset (skipping the first object already analized)
		for (int i = 1; i < dataset.size(); i++) {
			try {
			datasetObji = (JSONObject) dataset.get(i);
			double thisDouble = Double.parseDouble(datasetObji.get(key).toString());
			//if the value of the i-th object is < min, assign min to this value
			if (minmax[0] > thisDouble) {
				minmax[0] = (thisDouble + minmax[0])/2;
			} //if the value of the i-th object is > max, assign to max this value
			else if (minmax[1] < thisDouble) {
				minmax[1] = (thisDouble + minmax[1])/2;
			}
			} catch (Exception e) {
				//case null value
			}
		}
		return minmax;
	}
	
	public static void saveFile (String jsonContent) {
		@SuppressWarnings("resource")
		Scanner keyboard = new Scanner(System.in);
    	System.out.println("Do you want to save the generated config file? Press y to accept, press any other key to exit");
    	String response = keyboard.next();
    	if (response.equals("y")) {
    	try {
    		System.out.println("Type the file name: ");
        	String filen = keyboard.next();
            // Constructs a FileWriter given a file name, using the platform's default charset
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
}
