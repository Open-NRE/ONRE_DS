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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.iitd.cse.open_nre.onre.domain.OnrePatternTree;
import edu.iitd.cse.open_nre.onre.helper.OnreHelper_graph;
import edu.iitd.cse.open_nre.onre.runner.Onre_runMe;
import edu.iitd.cse.open_nre.onre_ds.helper.Onre_dsHelper;
import edu.iitd.cse.open_nre.onre_ds.helper.Onre_dsIO;
import edu.knowitall.tool.parse.graph.DependencyGraph;

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
		String filePath_inputSentences = "data/temp";
		String filePath_stopWords = "data/stopwords.txt";
		String filePath_jsonDepTrees = "data/jsonDepTrees";
		String filePath_invertedIndex = "data/invertedIndex";
		
		List<String> lines = Onre_dsIO.readFile(filePath_inputSentences);
		List<String> stopWords = Onre_dsIO.readFile(filePath_stopWords);
		
		List<String> jsonStrings = new ArrayList<String>();
		Map<String, Set<Integer>> invertedIndex = new HashMap<>();
		
		for (int i=0; i<lines.size(); i++) {

			System.out.println("::" + (i+1));
			String line = lines.get(i);
			
			helper_invertedIndex(stopWords, invertedIndex, i, line);
			jsonStrings.add(getJsonString(line));

			//onrePatternNode = gson.fromJson(jsonString, OnrePatternNode.class);
			//System.out.println();
		}
		
		Onre_dsIO.writeObjectToFile(filePath_invertedIndex, invertedIndex);
		Onre_dsIO.writeFile(filePath_jsonDepTrees, jsonStrings);
		
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

	private static String getJsonString(String line) {
		
		DependencyGraph depGraph = Onre_runMe.getDepGraph(line);
		DependencyGraph simplifiedGraph = OnreHelper_graph.simplifyGraph(depGraph);
		OnrePatternTree onrePatternTree = OnreHelper_graph.convertGraph2PatternTree(simplifiedGraph);
		Gson gson = new GsonBuilder().create();
		String jsonString = gson.toJson(onrePatternTree);
		return jsonString;
	}

}
