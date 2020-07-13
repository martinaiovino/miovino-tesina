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
	String filename = "mushrooms_red_acc";
	String jsonContent = "";
	try   
	{  
	//parsing a CSV file into BufferedReader class constructor
	@SuppressWarnings("resource")
	BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\Martina\\eclipse-workspace\\data_analysis\\\\src\\data_analysis\\" + filename + ".csv"));  
	Boolean first = true;
	String[] headers = new String[100];
	jsonContent += "[";
	int counter = 0;
	while ((line = br.readLine()) != null)   //returns a Boolean value  
	{
		if (first) {
			headers = line.split(splitBy);
			first = false;
		} else {
			String[] elements = line.split(splitBy);    // use comma as separator
			jsonContent += "{";
			for (int i = 0; i < elements.length - 1; i++) {
				jsonContent += ("\"" + headers[i] + "\" : \"" + elements[i] + "\", ");
			}
			jsonContent += ("\"" + headers[elements.length - 1] + "\": \"" + elements[elements.length - 1] + "\"}");
			System.out.println(counter);
			counter++;
		}
	}
	jsonContent += "]";
	}   
	catch (IOException e)   
	{  
	e.printStackTrace();  
	}
	
	//save file
	try {
	      FileWriter myWriter = new FileWriter("C:\\Users\\Martina\\eclipse-workspace\\data-analysis\\\\src\\com\\mars\\tesina\\" + filename + ".json");
	      myWriter.write(jsonContent);
	      myWriter.close();
	      System.out.println("Successfully wrote to the file.");
	    } catch (IOException e) {
	      System.out.println("An error occurred.");
	      e.printStackTrace();
	    }
	}  
}
