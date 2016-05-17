/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.runner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import opennlp.tools.util.InvalidFormatException;
import edu.iitd.cse.open_nre.onre.constants.OnreConstants;
import edu.iitd.cse.open_nre.onre.constants.OnreFilePaths;
import edu.iitd.cse.open_nre.onre.helper.OnreHelper_json;
import edu.iitd.cse.open_nre.onre.utils.OnreIO;
import edu.iitd.cse.open_nre.onre_ds.helper.Onre_dsHelper;
import edu.iitd.cse.open_nre.onre_ds.helper.Onre_dsIO;

/**
 * @author harinder
 *
 */
public class SerializeDepTree_file {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String filePath_inputSentences = "/home/harinder/Documents/IITD_MTP/numericSentencesKiKhoj/CluewebSeUmeed/0004wb/0004wb-18.sentences_filtered";
		/*String filePath_stopWords = "data/stopwords.txt";
		String filePath_jsonDepTrees = "data/jsonDepTrees";
		String filePath_invertedIndex = "data/invertedIndex";*/
		//String filePath_inputSentences = args[0];
		
		System.out.println("Starting with file: " + filePath_inputSentences);
		
		List<String> lines = OnreIO.readFile(filePath_inputSentences);
		List<String> stopWords = OnreIO.readFile(OnreFilePaths.filePath_stopWords);
		
		List<String> jsonStrings = new ArrayList<String>();
		Map<String, Set<Integer>> invertedIndex = new HashMap<>();
		
		for (int i=0; i<lines.size(); i++) {

			System.out.println("::" + (i+1));
			String line = lines.get(i);
			
			helper_invertedIndex(stopWords, invertedIndex, i, line);
			String jsonString = OnreHelper_json.getJsonString(line); 
			//if(jsonString!=null) 
				jsonStrings.add(jsonString);

			//onrePatternNode = gson.fromJson(jsonString, OnrePatternNode.class);
			//System.out.println();
		}
		
		Onre_dsIO.writeObjectToFile(filePath_inputSentences+OnreConstants.SUFFIX_INVERTED_INDEX, invertedIndex);
		OnreIO.writeFile(filePath_inputSentences+OnreConstants.SUFFIX_JSON_STRINGS, jsonStrings);
		
		//invertedIndex = (HashMap<String, Set<Integer>>)Onre_dsIO.readObjectFromFile(filePath_invertedIndex); 
		//System.out.println();
	}

	private static void helper_invertedIndex(List<String> stopWords,
			Map<String, Set<Integer>> invertedIndex, int i, String line) throws InvalidFormatException, IOException {
		//String []words = line.split(" "); //TO-DO: imp:tokenize here rather splitting
		String []words = Onre_dsHelper.tokenize(line);
		for (String word : words) {
			//word = word.replaceAll("\\.$", ""); //TO-DO: no need of this after tokenizing
			//word = word.replaceAll("\\,$", ""); //TO-DO: no need of this after tokenizing
			word = word.toLowerCase();
			
			if(stopWords.contains(word)) continue;
			Set<Integer> indexValue = invertedIndex.get(word);
			if(indexValue == null) indexValue = new TreeSet<>();
			indexValue.add(i);
			invertedIndex.put(word, indexValue);
		}
	}

}
