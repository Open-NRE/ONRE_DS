/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.iitd.cse.open_nre.onre.constants.OnreExtractionPartType;
import edu.iitd.cse.open_nre.onre.domain.OnrePatternNode;
import edu.iitd.cse.open_nre.onre.domain.OnrePatternTree;
import edu.iitd.cse.open_nre.onre_ds.domain.Onre_dsFact;
import edu.iitd.cse.open_nre.onre_ds.helper.Onre_dsHelper;
import edu.iitd.cse.open_nre.onre_ds.helper.Onre_dsIO;


/**
 * @author harinder
 *
 */
public class Onre_dsRunMe {

	/**
	 * @param args
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		System.out.println("I am running");
		//System.out.println(Onre_dsHelper.extractSIUnit("indian rupee"));
		String filePath_invertedIndex = "data/invertedIndex";
		String filePath_stopWords = "data/stopwords.txt";
		String filePath_seedfacts = "data/seedfacts.txt";
		String filePath_jsonDepTrees = "data/jsonDepTrees";
		String filePath_learnedDepPatterns = "data/learnedDepPatterns";
		
		List<String> stopWords = Onre_dsIO.readFile(filePath_stopWords);
		List<String> jsonDepTrees = Onre_dsIO.readFile(filePath_jsonDepTrees);
		
		Map<String, Set<Integer>> invertedIndex = (HashMap<String, Set<Integer>>)Onre_dsIO.readObjectFromFile(filePath_invertedIndex);
		
		List<Onre_dsFact> facts = Onre_dsHelper.readFacts(filePath_seedfacts);
		
		List<String> patterns = new ArrayList<>();
		for (Onre_dsFact fact : facts) {
			Set<Integer> intersection = getSentenceIdsWithMentionedFact(invertedIndex, fact, stopWords);
			for (Integer id : intersection) {
				String jsonDepTree = jsonDepTrees.get(id);
				OnrePatternTree onrePatternTree = getOnrePatternTree(jsonDepTree);
				patterns.add(makePattern(onrePatternTree, fact));
			}
		}
		
		Onre_dsIO.writeFile(filePath_learnedDepPatterns, patterns);
		System.out.println("----Done----");
	}

	private static OnrePatternTree getOnrePatternTree(String jsonDepTree) {
		Gson gson = new GsonBuilder().create();
		OnrePatternTree onrePatternTree = gson.fromJson(jsonDepTree, OnrePatternTree.class);
		
		OnrePatternNode root = onrePatternTree.root;
		
		Queue<OnrePatternNode> myQ = new LinkedList<>();
		myQ.add(root);
		
		while(!myQ.isEmpty()) {
			OnrePatternNode currNode = myQ.remove();
			
			for(OnrePatternNode child : currNode.children) {
				child.parent = currNode;
				myQ.add(child);
			}
		}
		
		return onrePatternTree;
	}

	private static Set<Integer> getSentenceIdsWithMentionedFact(
			Map<String, Set<Integer>> invertedIndex, Onre_dsFact fact, List<String> stopWords) {
		
		List<Set<Integer>> listOfSetOfsentenceIds = new ArrayList<Set<Integer>>();
		
		for (String factWord : fact.words) {
			//factWord = factWord.toLowerCase();
			if(factWord.trim().isEmpty()) continue;
			if(stopWords.contains(factWord)) continue;
			
			Set<Integer> setOfsentenceIds = invertedIndex.get(factWord);
			listOfSetOfsentenceIds.add(setOfsentenceIds);
		}
		
		Set<Integer> intersection = listOfSetOfsentenceIds.get(0);
		for (int i = 1; i < listOfSetOfsentenceIds.size(); i++) {
			intersection.retainAll(listOfSetOfsentenceIds.get(i));
		}
		
		return intersection;
	}
	
	private static String makePattern(OnrePatternTree onrePatternTree, Onre_dsFact fact) {
		OnrePatternNode argNode = searchNode_markVisited(onrePatternTree, fact.words[0], OnreExtractionPartType.ARGUMENT);
		OnrePatternNode relNode = searchNode_markVisited(onrePatternTree, fact.words[1], OnreExtractionPartType.RELATION);
		OnrePatternNode qValueNode = searchNode_markVisited(onrePatternTree, fact.words[2], OnreExtractionPartType.QUANTITY);
		OnrePatternNode qUnitNode = searchNode_markVisited(onrePatternTree, fact.words[3], OnreExtractionPartType.QUANTITY);
		
		argNode.word = "{arg}";
		relNode.word = "{rel}";
		qValueNode.word = "{quantity}";
		qUnitNode.word = "{quantity}";
		
		OnrePatternNode LCA = findLCA(onrePatternTree);
		StringBuilder sb_pattern = new StringBuilder();
		sb_pattern.append("<");
		makePattern_helper(LCA, sb_pattern);
		sb_pattern.append(">");
		String pattern = sb_pattern.toString();
		pattern = patternPostProcessing(pattern);
		return pattern;
	}

	private static String patternPostProcessing(String pattern) {
		String str_isAndOthers = "#is|are|was|were#";
		String str_hasAndOthers = "#has|have|had#";
		String str_noun = "#NNP|NN)";
		
		//System.out.println(pattern);
		pattern = pattern.replaceAll("<>", "");
		//System.out.println(pattern);
		pattern = pattern.replaceAll("null", "");
		//System.out.println(pattern);

		pattern = pattern.replaceAll("#is#", str_isAndOthers);
		pattern = pattern.replaceAll("#are#", str_isAndOthers);
		pattern = pattern.replaceAll("#was#", str_isAndOthers);
		pattern = pattern.replaceAll("#were#", str_isAndOthers);
		//System.out.println(pattern);
		
		pattern = pattern.replaceAll("#has#", str_hasAndOthers);
		pattern = pattern.replaceAll("#have#", str_hasAndOthers);
		pattern = pattern.replaceAll("#had#", str_hasAndOthers);
		//System.out.println(pattern);
		
		pattern = pattern.replaceAll("#NNP\\)", str_noun);
		pattern = pattern.replaceAll("#NN\\)", str_noun);
		//System.out.println(pattern);
		
		pattern = pattern.replaceAll("#\\{arg\\}#NNP\\|NN\\)", "#{arg}#NNP|NN|PRP)");
		//System.out.println(pattern);
		
		return pattern;
	}
	
	private static void makePattern_helper(OnrePatternNode node, StringBuilder sb_pattern) {
		
		sb_pattern.append(Onre_dsHelper.getPatternNodeString(node));
		
		if(node.children!=null && node.children.size()!=0) sb_pattern.append("<");
		for(OnrePatternNode child : node.children) {
			if(child.visitedCount == 0) continue;
			makePattern_helper(child, sb_pattern);
		}
		if(node.children!=null && node.children.size()!=0) sb_pattern.append(">");
		
	}
	
	private static OnrePatternNode searchNode(OnrePatternTree onrePatternTree, String word, OnreExtractionPartType partType) {
		OnrePatternNode root = onrePatternTree.root;
		
		Queue<OnrePatternNode> myQ = new LinkedList<>();
		myQ.add(root);
		
		while(!myQ.isEmpty()) {
			OnrePatternNode currNode = myQ.remove();
			if(currNode.word.equalsIgnoreCase(word)) return currNode;
			
			List<OnrePatternNode> children = currNode.children;
			for (OnrePatternNode child : children) {
				myQ.add(child);
			}
		}
		
		System.err.println("---It shall never come here...problem, exiting---");
		System.exit(1);
		return null;
	}
	
	private static void markVisited(OnrePatternNode node) {
		OnrePatternNode temp = node;
		while(temp !=null) {
			temp.visitedCount++;
			temp = temp.parent;
		}
	}
	
	private static OnrePatternNode searchNode_markVisited(OnrePatternTree onrePatternTree, String word, OnreExtractionPartType partType) {
		OnrePatternNode node = searchNode(onrePatternTree, word, partType);
		markVisited(node);
		node.nodeType = partType;
		return node;
	}
	
	private static OnrePatternNode findLCA(OnrePatternTree onrePatternTree) {
		OnrePatternNode root = onrePatternTree.root;
		
		OnrePatternNode LCA = null;
		
		Queue<OnrePatternNode> myQ = new LinkedList<>();
		myQ.add(root);
		
		while(!myQ.isEmpty()) {
			OnrePatternNode currNode = myQ.remove();
			if(currNode.visitedCount == 4) LCA = currNode;
			
			List<OnrePatternNode> children = currNode.children;
			for (OnrePatternNode child : children) {
				myQ.add(child);
			}
		}
		
		return LCA;
	}
}

