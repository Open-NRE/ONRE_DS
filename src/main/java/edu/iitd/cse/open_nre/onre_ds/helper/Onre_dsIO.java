/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;



/**
 * @author harinder
 *
 */
public class Onre_dsIO {

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
