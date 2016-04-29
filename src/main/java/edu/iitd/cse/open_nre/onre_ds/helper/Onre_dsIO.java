/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;



/**
 * @author harinder
 *
 */
public class Onre_dsIO {

	public static List<String> readFile(String filePath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		
		List<String> lines = new ArrayList<>();
		
		String line = br.readLine();
		while(line != null) {
			if(!line.trim().isEmpty()) lines.add(line); 
			line = br.readLine();
		}
		
		br.close();
		return lines;
	}
	
	public static void writeFile(String filePath, List<String> lines) throws IOException {
		PrintWriter pw = new PrintWriter(filePath);
		for (String string : lines) pw.println(string);
		pw.close();
	}
	
	public static void writeObjectToFile(String filePath, Object obj) throws IOException {
	     ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(new File(filePath)));
	     s.writeObject(obj);
	     s.close();
	}
	
	public static Object readObjectFromFile(String filePath) throws IOException, ClassNotFoundException {
	    ObjectInputStream s = new ObjectInputStream(new FileInputStream(new File(filePath)));
	    Object fileObj = s.readObject();
	    s.close();
	    return fileObj;
	}
	
}
