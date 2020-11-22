package project;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CSVToJSON {
	public static void main(String[] args)   
	{  
	String line = "";  
	String splitBy = ",";
	String filename = "chicagoTrafficTracker_con1";
	String jsonContent = "";
	try   
	{  
	//parsing a CSV file into BufferedReader class constructor
	@SuppressWarnings("resource")
	BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\Martina\\Desktop\\" + filename + ".csv"));  
	Boolean first = true;
	String[] headers = new String[100];
	jsonContent += "[";
	int counter = 0;
	while ((line = br.readLine()) != null)   //returns a Boolean value  
	{
		if (first) {
			headers = line.replace("\"", "").split(splitBy);
			first = false;
		} else {
			String[] elements = line.replace("\"", "").split(splitBy);    // use comma as separator
			jsonContent += "{";
			for (int i = 0; i < elements.length - 1; i++) {
				if (elements[i].contentEquals("null")) {
					jsonContent += ("\"" + headers[i] + "\" : " + elements[i] + ", ");
				} else {
					jsonContent += ("\"" + headers[i] + "\" : \"" + elements[i] + "\", ");
				}
			}
			if (elements[elements.length - 1].contentEquals("null")) {
				jsonContent += ("\"" + headers[elements.length - 1] + "\": " + elements[elements.length - 1] + "},");
			} else {
				jsonContent += ("\"" + headers[elements.length - 1] + "\": \"" + elements[elements.length - 1] + "\"},");
			}
			System.out.println(counter);
			counter++;
		}
	}
	jsonContent = jsonContent.substring(0, jsonContent.length() - 1) + "]";
	} 
	catch (IOException e)   
	{  
	e.printStackTrace();  
	}
	
	//save file
	try {
	      FileWriter myWriter = new FileWriter("C:\\Users\\Martina\\Desktop\\" + filename + ".json");
	      myWriter.write(jsonContent);
	      myWriter.close();
	      System.out.println("Successfully wrote to the file.");
	    } catch (IOException e) {
	      System.out.println("An error occurred.");
	      e.printStackTrace();
	    }
	}  
}
