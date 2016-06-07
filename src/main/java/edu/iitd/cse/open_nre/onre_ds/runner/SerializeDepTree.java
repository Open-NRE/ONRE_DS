/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.runner;

import java.io.File;
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
import edu.iitd.cse.open_nre.onre.domain.Onre_dsDanrothSpans;
import edu.iitd.cse.open_nre.onre.helper.OnreHelper_DanrothQuantifier;
import edu.iitd.cse.open_nre.onre.helper.OnreHelper_json;
import edu.iitd.cse.open_nre.onre.utils.OnreIO;
import edu.iitd.cse.open_nre.onre.utils.OnreUtils;
import edu.iitd.cse.open_nre.onre_ds.helper.Onre_dsHelper;
import edu.iitd.cse.open_nre.onre_ds.helper.Onre_dsIO;

/**
 * @author harinder
 *
 */
public class SerializeDepTree {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		File folder = new File(args[0]);
		
		Set<String> files = new TreeSet<>();
		OnreUtils.listFilesForFolder(folder, files);
		
		List<String> stopWords = OnreIO.readFile_classPath(OnreFilePaths.filePath_stopWords);
		for (String file : files) {
			if(!file.endsWith("_filtered")) continue;
			
			System.out.println("Starting with file: " + file);
			
			List<String> lines = OnreIO.readFile(file);
			
			List<String> jsonStrings_patternTree = new ArrayList<String>();
			List<String> jsonStrings_danrothSpans = new ArrayList<String>();
			Map<String, Set<Integer>> invertedIndex = new HashMap<>();
			
			for (int i=0; i<lines.size(); i++) {
	
				if(i%1000==0) System.out.println("::" + i);
				String line = lines.get(i);
				
				helper_invertedIndex(stopWords, invertedIndex, i, line); //TODO: IMP:uncomment
				helper_patternTree(jsonStrings_patternTree, line); //TODO: IMP:uncomment
				helper_danrothSpans(jsonStrings_danrothSpans, line);
			}
			
			Onre_dsIO.writeObjectToFile(file+OnreConstants.SUFFIX_INVERTED_INDEX, invertedIndex);//TODO: IMP:uncomment 
			OnreIO.writeFile(file+OnreConstants.SUFFIX_JSON_STRINGS, jsonStrings_patternTree);//TODO: IMP:uncomment 
			OnreIO.writeFile(file+OnreConstants.SUFFIX_DANROTH_SPANS, jsonStrings_danrothSpans);//TODO: IMP:uncomment
		}
	}

	private static void helper_danrothSpans(List<String> jsonStrings_danrothSpans, String line) {
		Onre_dsDanrothSpans onre_dsDanrothSpans=OnreHelper_DanrothQuantifier.getQuantitiesDanroth(line);
		String jsonString_quantSpans=OnreHelper_json.getJsonStringForObject(onre_dsDanrothSpans);
		jsonStrings_danrothSpans.add(jsonString_quantSpans);
	}

	private static void helper_patternTree(List<String> jsonStrings_patternTree, String line) {
		String jsonString_patternTree = OnreHelper_json.getJsonString_patternTree(line); 
		jsonStrings_patternTree.add(jsonString_patternTree); 
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
