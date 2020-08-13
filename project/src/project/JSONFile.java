package project;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class JSONFile {
	
	static long dataFileDiff;
	static long nowMillis;
	static List<Boolean> arrayBoolCateg;
	static List<Boolean> arrayBoolFloat;
	static List<Boolean> arrayBoolNotNull;
	static List<Double> accuracyDistance;
	static double consistency_evaluation,completeness_missing,accuracy_evaluation_boolean_float, timeliness_evaluation, accuracy_evaluation_boolean_categ;
	
	/* Method to read a json file. It returns an object.
	 * This method will be used for reading both configuration and dataset files. */
	public static Object readFile(String fileName) {
		System.out.println("File to analyse: " + fileName);
        try
        {
        	File f = new File("resources/" + fileName); // look for the file
        	System.out.println(f.getAbsolutePath());
        	JSONParser jsonParser = new JSONParser(); // parser
        	FileReader reader = new FileReader(f.getAbsolutePath());
            Object fileObject = jsonParser.parse(reader);
            
            if(f.exists() && !fileName.endsWith("_config.json")) { // if there is a configuration file
            	long lastModifiedMillis = f.lastModified(); // get the last modified date in milliseconds
            	nowMillis = System.currentTimeMillis(); // current milliseconds
            	dataFileDiff = nowMillis - lastModifiedMillis; // current milliseconds - last modified milliseconds
            	System.out.println("Last modified millis:" + lastModifiedMillis);
            	System.out.println("Now millis: " + nowMillis + "; Diff millis: " + dataFileDiff);
            }
			return fileObject; // return object
        } catch (Exception e) { //ParseException or FileNotFoundException
            e.printStackTrace();
            return null;
        }
	}
	
	/* Check whether a value is in inside an interval min-max.
	 * Here I also calculate how much the value is close to the interval */
	public static void interval (double min, double max, double value) {
		double interval = max - min; // interval
		
		if (value >= min && value <= max) {
			arrayBoolFloat.add(true); // data inside the interval
		} else {
			arrayBoolFloat.add(false); // data outside the interval
		}
		
		//for the accuracy distance
		double mean = interval / 2; //mean
		double computation = Math.max(0, 1 - Math.abs((value - mean) / (interval * 0.5)));
		accuracyDistance.add(computation);
	}
	
	// Check whether a value is equal to any of the elements of the interval array
	public static void interval (String[] array, String value) {
		Boolean found = false;
		for (String n : array) {
            if (n.contentEquals(value)) {
                found = true;
                break;
            }
        }

        if(found) {
        	arrayBoolCateg.add(true);
        }
        else {
        	arrayBoolCateg.add(false);
        }
	}
	
	/* The following method will be used for calculating accuracy_float, accuracy_categ and completeness
	 * knowing the list of booleans previously built */
	public static double accuracy_float_categ_completeness (List<Boolean> arrayBool, String metric) {
		//for accuracy for array and min-max -> correct = correct values found
		//for completeness -> correct = not null values
		int tot = arrayBool.size();
		int correct = 0;
		for (int i = 0; i < tot; i++) {
			if(arrayBool.get(i)) {
				correct++;
			}
		}
		double accuracy = (double) correct / tot;
		System.out.println(metric + ": " + accuracy);
		return accuracy;
	}

	// Calculating the accuracy of all the dataset, regardless the float or categ constraints
	public static double accuracy () {
		int tot = arrayBoolFloat.size() + arrayBoolCateg.size();
		int correct = 0;
		for (Boolean x : arrayBoolFloat) {
			if (x) {
				correct++;
			}
		}
		for (Boolean y : arrayBoolCateg) {
			if (y) {
				correct++;
			}
		}
		
		double accuracy = (double) correct / tot;
		System.out.println("ACCURACY: " + accuracy);
		return accuracy;
	}
	
	// Accuracy distance calculation
	public static double accuracy_evaluation_distance () {
		//for min max
		double acc_distance = 0;
		for (int i = 0; i < accuracyDistance.size(); i++) {
			acc_distance = acc_distance + accuracyDistance.get(i);
		}
		acc_distance = acc_distance / accuracyDistance.size();
		System.out.println("ACCURACY_DISTANCE: " + acc_distance);
		return acc_distance;
	}
	
	// Timeliness
	public static double timeliness (long volatility) {
		System.out.println("TIMELINESS: " + Math.max(0, 1 - (dataFileDiff/volatility)));
		return Math.max(0, 1 - (dataFileDiff/volatility));
	}
	
	@SuppressWarnings("unchecked")
	public static String buildDQEvaluator(String sourceType) {
		JSONObject DQEvaluator = new JSONObject(); //DQEvaluator
    	
		JSONObject global = new JSONObject();
    	global.put("consistency", consistency_evaluation);
    	global.put("completeness_missing", completeness_missing);
    	
    	JSONObject attribute = new JSONObject();
    	JSONObject accuracy = new JSONObject();
    	accuracy.put("accuracy", accuracy_evaluation_boolean_float);
    	attribute.put("float", accuracy);
    	
    	JSONObject timeliness = new JSONObject();
    	timeliness.put("timeliness", timeliness_evaluation);
    	attribute.put("timestamp", timeliness);
    	
    	JSONObject accuracyj = new JSONObject();
    	accuracyj.put("accuracy", accuracy_evaluation_boolean_categ);
    	attribute.put("categ", accuracyj);
    	
    	JSONObject sourcetype = new JSONObject();
    	sourcetype.put("attribute", attribute);
    	sourcetype.put("global", global);
    	DQEvaluator.put(sourceType, sourcetype);
    	
    	System.out.println(DQEvaluator.toString());
		
    	try {
            // Constructs a FileWriter given a file name, using the platform's default charset
            FileWriter file = new FileWriter("resources/d.json");
            file.write(DQEvaluator.toJSONString());
            file.flush();
            file.close();
            System.out.println("File saved! Refresh the resources folder");
        } catch (IOException e) {
            e.printStackTrace();
        }
    	return DQEvaluator.toJSONString();
	}
	
	/*----------------      Methods for the stream metrics        ----------------*/
	
	/* Method to divide the dataset into batches. The dataset will be inserted inside a matrix,
	 * where each row is the batch. The matrix has as many columns as the window size 
	 * previously set. */
	public static JSONObject[][] separe_windows(int window_size, JSONArray datasetJSONArray) {
		int dataset_size = datasetJSONArray.size();
		int number_of_windows = Math.floorDiv(dataset_size + (window_size - 1), window_size);
		JSONObject[][] subsets = new JSONObject[number_of_windows][window_size];
		System.out.println("dsize: " + dataset_size + " nwin: " + number_of_windows);
		int count = 0;
		for (int i = 0; i < number_of_windows; i++) { //rows
			for (int j = 0; j < window_size; j++) { //columns
				subsets[i][j] = (JSONObject) datasetJSONArray.get(count);
				System.out.println("subset[" + i + "][" + j + "]: " + datasetJSONArray.get(count));
				count++;
				if (count == dataset_size) {
					break; // dataset elements finished
				}
			}
		}
		return subsets;
	}
	
	// Method for calculating the accuracy category and float, and the completeness for stream data
	public static double accuracy_categ_float_stream (double number_of_windows, ArrayList<Double> accuracySubsets, String metric) {
		double sumAccuracySubsets = 0;
		for (double elem : accuracySubsets) {
			System.out.print(elem + " ");
			sumAccuracySubsets += elem;
		}
		double accuracy = 1 / (number_of_windows - ((number_of_windows - 1) / 2)) * sumAccuracySubsets;
		System.out.println("sum : " + sumAccuracySubsets + "; " + metric + " : " + accuracy);
		return accuracy;
	}
	
	public static double timeliness (JSONObject configJSONObject, JSONArray datasetJSONArray) {
		// From the configuration file, get the datatypes object
		double timelness = 0;
		JSONObject timestampVal = (JSONObject) configJSONObject.get("datatypes");
		for (Object v : timestampVal.keySet()) { // iterate inside the datatypes
			//System.out.println("key: " + v); // key
			String subval = (String) timestampVal.get(v); // value
			//System.out.println("value: " + subval);
			try {
				if (subval.equals("timestamp")) { // if a value of type timestamp is found
					List<Long> currencies = new ArrayList<>();
					long volatility = (long) configJSONObject.get("volatility");
					// iterate in the dataset
					for (int i = 0; i < datasetJSONArray.size(); i++) {
						JSONObject currentElem = (JSONObject) datasetJSONArray.get(i);
						//get the date value of the i-th element of the dataset
						Date date = new SimpleDateFormat("yyyy-MM-dd").parse(currentElem.get(v).toString());
						long millis = date.getTime(); // milliseconds of the timestamp i-th value
						currencies.add(nowMillis-millis); // add to the array of currencies nowMillis-millis
						}
						List<Long> timeliness = new ArrayList<>();
						long sum = 0;
						for (int i = 0; i < currencies.size(); i++) {
							timeliness.add(Math.max(0, 1-currencies.get(i)/volatility));
							sum += timeliness.get(i);
						}
						System.out.println("TIMELINESS: " + sum/timeliness.size());
						timelness = sum/timeliness.size();
					}
					} catch (Exception e) {
						// Auto-generated catch block
						e.printStackTrace();
					}
				}
				return timelness;
	}
			
	/* --------------------    Calculate batch metrics    -------------------- */
	public static void calculateBatchMetrics(JSONObject configJSONObject, JSONArray datasetJSONArray) {
		arrayBoolCateg = new ArrayList<>();
		arrayBoolFloat = new ArrayList<>();
		arrayBoolNotNull = new ArrayList<>();
		accuracyDistance = new ArrayList<>();
		
		// Get values_range from the configuration object
		JSONObject valuesRangeObj = (JSONObject) configJSONObject.get("values_range");
		for (Object v : valuesRangeObj.keySet()) { // iterate inside the values_range
			//System.out.println("key: " + v); // key
			JSONObject subval = (JSONObject) valuesRangeObj.get(v);
			//System.out.print("interval: " + subval.get("interval")); //get the interval corresponding to that key
			
			// Building up the arrays of booleans for accuracy categ, float, not null and distance
			if (subval.get("interval") instanceof JSONObject) { // case 1: interval is a min-max
				//System.out.println(" MIN-MAX");
				JSONObject subv = (JSONObject) subval.get("interval");
				Double minvalue = Double.parseDouble(subv.get("min").toString()); //minimum value
				Double maxvalue = Double.parseDouble(subv.get("max").toString()); //maximum value
				// Iterate in the dataset object and check whether the key respects the interval constraints
				for (int i = 0; i < datasetJSONArray.size(); i++) { 
					JSONObject datasetObji = (JSONObject) datasetJSONArray.get(i);
					try {
						interval(minvalue, maxvalue, Double.parseDouble(datasetObji.get(v).toString()));
						arrayBoolNotNull.add(true);
					} catch (NullPointerException e) {
						arrayBoolNotNull.add(false);
					}
				}	
			} else { // case 2: interval is an array of values
				//System.out.println(" ARRAY");
				String arrayToString = subval.get("interval").toString();
				String arrayEdits = arrayToString.substring(1, arrayToString.length() - 1).replace("\"", "");
				String[] valuesRange = arrayEdits.split(",");
				// Iterate in the dataset object and check whether the key respects the interval constraints
				for (int i = 0; i < datasetJSONArray.size(); i++) {
					JSONObject datasetObji = (JSONObject) datasetJSONArray.get(i);
					try {
						interval(valuesRange, (String) datasetObji.get(v));
						arrayBoolNotNull.add(true);
					} catch (NullPointerException e) {
						arrayBoolNotNull.add(false);
					}
				}
			}
		}
		
		// Computing the metrics
		accuracy_evaluation_boolean_categ = accuracy_float_categ_completeness(arrayBoolCateg, "ACCURACY_CATEG"); // Accuracy category
		accuracy_evaluation_boolean_float = accuracy_float_categ_completeness(arrayBoolFloat, "ACCURACY_FLOAT"); // Accuracy float
		completeness_missing = accuracy_float_categ_completeness(arrayBoolNotNull, "COMPLETENESS_MISSING"); // Completeness missing
		accuracy_evaluation_distance(); // Accuracy evaluation distance
		accuracy(); // Accuracy (generic)
		try {
			long volatility = (long) configJSONObject.get("volatility"); // Volatility
			timeliness_evaluation = timeliness(volatility);
		} catch (Exception e) {
			System.out.println("NO VOLATILITY");
		}
	}
	
	/* --------------------    Calculate stream metrics    -------------------- */
	public static void calculateStreamMetrics(JSONObject configJSONObject, JSONArray datasetJSONArray) {
		
		int window_size = 10; // set windows size
		int dataset_size = datasetJSONArray.size(); // get dataset size
		// Compute the number of windows depending on the window size and dataset size
		int number_of_windows = Math.floorDiv(dataset_size + (window_size - 1), window_size); 
		JSONObject[][] separewin = separe_windows(window_size, datasetJSONArray); // Separe the dataset
		
		ArrayList<Double> accuracy_float_subsets = new ArrayList<>();
		ArrayList<Double> accuracy_categ_subsets = new ArrayList<>();
		ArrayList<Double> completeness_subsets = new ArrayList<>();
		
		// From the configuration file, get the values range
		JSONObject valuesRangeObj = (JSONObject) configJSONObject.get("values_range");
		for (int i = 0; i < number_of_windows; i++) {
			arrayBoolFloat = new ArrayList<>();
			arrayBoolNotNull = new ArrayList<>();
			arrayBoolCateg = new ArrayList<>();
			accuracyDistance = new ArrayList<>();
			System.out.println("\nWindow " + (i + 1) + ": ");
			// For each batch of data, build up the lists of bool
			for (int j = 0; j < window_size; j++) {
				try {
					for (Object v : separewin[i][j].keySet()) {
						try {
						//System.out.println("i " + i + " j " + j);
						//System.out.println("KEYY: " + v);
						JSONObject subval = (JSONObject) valuesRangeObj.get(v);
						if (subval.get("interval") instanceof JSONObject) {
							//System.out.println(" MIN-MAX");
							JSONObject subv = (JSONObject) subval.get("interval");
							Double minvalue = Double.parseDouble(subv.get("min").toString()); //minimum value
							Double maxvalue = Double.parseDouble(subv.get("max").toString()); //maximum value
							try {
								interval(minvalue, maxvalue, Double.parseDouble(separewin[i][j].get(v).toString()));
								arrayBoolNotNull.add(true);
							} catch (NullPointerException e) {
								arrayBoolNotNull.add(false);
							}
						} else {
							//System.out.println(" ARRAY");
							String arrayToString = subval.get("interval").toString();
							String arrayEdits = arrayToString.substring(1, arrayToString.length() - 1).replace("\"", "");
							String[] valuesRange = arrayEdits.split(",");
							try {
								interval(valuesRange, (String) separewin[i][j].get(v));
								arrayBoolNotNull.add(true);
							} catch (Exception e) {
								arrayBoolNotNull.add(false);
							}
						}
						} catch (Exception e) {
							//System.out.println("No constraints");
						}
					} 
				} catch (Exception e) {
					//System.out.println("Finished");
				}
			}
			// i-th batch is finished. Compute the metrics for the i-th batch
			double accuracy_float_subset = accuracy_float_categ_completeness(arrayBoolFloat, "ACCURACY_FLOAT");
			double weighted_accuracy_float = (accuracy_float_subset * (i + 1)) / number_of_windows;
			accuracy_float_subsets.add(weighted_accuracy_float);
			
			double accuracy_categ_subset = accuracy_float_categ_completeness(arrayBoolCateg, "ACCURACY_CATEG");
			double weighted_accuracy_categ = (accuracy_categ_subset * (i + 1)) / number_of_windows;
			accuracy_categ_subsets.add(weighted_accuracy_categ);
			
			double completeness_subset = accuracy_float_categ_completeness(arrayBoolNotNull, "COMPLETENESS");
			double weighted_completeness = (completeness_subset * (i + 1)) / number_of_windows;
			completeness_subsets.add(weighted_completeness);
		}
		System.out.println("");
		// compute the stream metrics
		accuracy_evaluation_boolean_float = accuracy_categ_float_stream(number_of_windows, accuracy_float_subsets, "STREAM ACCURACY_FLOAT");
		accuracy_evaluation_boolean_categ = accuracy_categ_float_stream(number_of_windows, accuracy_categ_subsets, "STREAM ACCURACY_CATEG");
		completeness_missing = accuracy_categ_float_stream(number_of_windows, completeness_subsets, "STREAM COMPLETENESS");
		
		timeliness_evaluation = timeliness(configJSONObject, datasetJSONArray);
	}
	
	/*----------------         MAIN             ----------------*/
	public static void main (String[] args) {
		String fileName = "blood_analysis"; // File name
		
		// Read the dataset and configuration files
		Object returnedDataset = readFile(fileName + ".json"); // dataset object
		Object returnedConfig = readFile(fileName + "_config.json"); //configuration file object
		String result = "Unknown data type";
		
		//Check whether the two objects are not null
		if (returnedDataset != null && returnedConfig != null) {
			JSONArray datasetJSONArray = (JSONArray) returnedDataset; // dataset JSON Array
			System.out.println("Total number of data: " + datasetJSONArray.size() + "\n"); // total number of data
			
			JSONObject configJSONObject = (JSONObject) returnedConfig; // configuration JSON Object
			
			prepare_consistency(configJSONObject, datasetJSONArray);
			
			//Check the source type
			String sourceType = (String) configJSONObject.get("source_type");
			//Source type can be either batch or stream
			if (sourceType.equals("batch")) {
				calculateBatchMetrics(configJSONObject, datasetJSONArray); 
				result = buildDQEvaluator("batch");
			} else {
				if (sourceType.equals("stream")) {
					calculateStreamMetrics(configJSONObject, datasetJSONArray);
					result = buildDQEvaluator("stream");
				}
			}
			System.out.println(result);
		} else {
			System.out.println("Files not found");
		}
	}
	
	/************************************************************************************
	 * **********************************************************************************
	 * **********************************************************************************/
	public static void prepare_consistency (JSONObject configJSONObject, JSONArray dataset) {
		JSONArray associationRules = (JSONArray) configJSONObject.get("association_rules");
		ArrayList<Double> consistency = new ArrayList<>();
		//System.out.println(associationRules.size());
		JSONObject valuesRangeObj = (JSONObject) configJSONObject.get("values_range");
		for (Object x : associationRules) {
			JSONArray y = (JSONArray) x;
			Boolean[] array = new Boolean[dataset.size()];
			Arrays.fill(array, true);
			for (Object z : y) {
				String elem = z.toString().substring(2, z.toString().length()-2).replace("\\", "");
				//System.out.println("y: " + y.toString() + "z " + elem);
				JSONObject valuesRange = (JSONObject) valuesRangeObj.get(elem);
				if(valuesRange.get("interval") instanceof JSONObject) {
					//MIN-MAX
					JSONObject subv = (JSONObject) valuesRange.get("interval");
					Double minvalue = Double.parseDouble(subv.get("min").toString()); //minimum value
					Double maxvalue = Double.parseDouble(subv.get("max").toString()); //maximum value
					for (int i = 0; i < dataset.size(); i++) {
						try {
						JSONObject element = (JSONObject) dataset.get(i);
						if (Double.parseDouble(element.get(elem).toString()) >= minvalue && Double.parseDouble(element.get(elem).toString()) <= maxvalue) {
							array[i] = (array[i] == true);
						} else {
							array[i] = (array[i] == false);
						}
						} catch(Exception e) {
							array[i] = false;
						}
					}
				} else {
					//ARRAY
					String arrayToString = valuesRange.get("interval").toString();
					String arrayEdits = arrayToString.substring(1, arrayToString.length() - 1).replace("\"", "");
					String[] vRange = arrayEdits.split(",");
					for (int i = 0; i < dataset.size(); i++) {
						try {
							JSONObject element = (JSONObject) dataset.get(i);
							String vali = element.get(elem).toString();
							Boolean found = false;
							for (String n : vRange) {
					            if (n.contentEquals(vali)) {
					                found = true;
					            }
					        }
							array[i] = (array[i] && found);
						} catch (Exception e) {
							array[i] = false;
						}
					}
				}
				/*if(elem.contains("\",\"")) {
					String[] subelems = elem.split("\",\"");
					for (String i : subelems) {
						System.out.println(i);
					}
				}*/
			}
			int correct = 0;
			for (int i = 0; i < array.length; i++) {
				if (array[i]) {
					correct++;
				}
			}
			consistency.add((double) correct/array.length);
			//System.out.println((double) correct/array.length);
		}
		double sum = 0;
		for (int i = 0; i < consistency.size(); i++) {
			sum += consistency.get(i);
		}
		consistency_evaluation = sum/consistency.size();
		System.out.println("CONSISTENCY: " + consistency_evaluation);
	}
	/************************************************************************************
	 * **********************************************************************************
	 * **********************************************************************************/
}