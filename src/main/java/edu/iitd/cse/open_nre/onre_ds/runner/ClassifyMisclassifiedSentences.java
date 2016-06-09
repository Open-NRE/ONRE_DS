/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.runner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.iitd.cse.open_nre.onre.utils.OnreIO;
import edu.iitd.cse.open_nre.onre_ds.helper.Onre_dsHelper;

/**
 * @author swarnadeep
 *
 */

public class ClassifyMisclassifiedSentences {
	
	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String inputFile = args[0];
		
		List<String> lines = OnreIO.readFile(inputFile);
		Map<Integer, Integer> correctlyClassified = new HashMap<>();
		Map<Integer, Integer> incorrectlyClassified = new HashMap<>();
		
		int lineCount = 0;
		
		while(lineCount < lines.size()) {
			String line = lines.get(lineCount);
			String []words = Onre_dsHelper.tokenize(line);
			if(words.length == 1 && !words[0].contains("::")) {
				Integer patternNumber = Integer.valueOf(words[0]);
				
				++lineCount;
				String nextLine = lines.get(lineCount); // get the next line
				words = Onre_dsHelper.tokenize(nextLine);
				
				populateMaps(correctlyClassified, incorrectlyClassified, patternNumber, words[words.length-1]);
			}
			++lineCount;
		}
		
		String outputFile = args[1];
		writeCountsToFile(outputFile, correctlyClassified, incorrectlyClassified);
	}
	
	private static void populateMaps(Map<Integer, Integer> correctlyClassified, 
			Map<Integer, Integer> incorrectlyClassified, Integer patternNumber, String annotation) {
		
		if(annotation.equals("T")) {
			if(correctlyClassified.containsKey(patternNumber)) {
				Integer correctCount = correctlyClassified.get(patternNumber);
				correctlyClassified.put(patternNumber, correctCount+1);
			} 
			else {
				correctlyClassified.put(patternNumber, 1);
			}
		} 
		else if(annotation.equals("F")) {
			if(incorrectlyClassified.containsKey(patternNumber)) {
				Integer incorrectCount = incorrectlyClassified.get(patternNumber);
				incorrectlyClassified.put(patternNumber, incorrectCount+1);
			} 
			else {
				incorrectlyClassified.put(patternNumber, 1);
			}
		}
		else {
			System.out.println("Something wrong happened");
			System.exit(0);
		}
	}
	
	private static void writeCountsToFile(String outputFile, Map<Integer, Integer> correctlyClassified, 
			Map<Integer, Integer> incorrectlyClassified) throws FileNotFoundException {
		
		PrintWriter pw = new PrintWriter(outputFile);
		
		for(Integer patternNumber : correctlyClassified.keySet()) {
			Integer correctCount = correctlyClassified.get(patternNumber);
			Integer incorrectCount = 0;
			
			if(incorrectlyClassified.containsKey(patternNumber)) {
				incorrectCount = incorrectlyClassified.get(patternNumber);
			}
			
			Double correctRatio = (Double.valueOf(correctCount)/(Double.valueOf(correctCount) + 
					Double.valueOf(incorrectCount)));
			
			pw.println(patternNumber + ";" + correctCount + ";" + incorrectCount + ";" + correctRatio);
		}
		pw.close();
	}

}
