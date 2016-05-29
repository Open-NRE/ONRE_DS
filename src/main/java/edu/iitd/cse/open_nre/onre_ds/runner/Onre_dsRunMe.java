/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.iitd.cse.open_nre.onre.constants.OnreConstants;
import edu.iitd.cse.open_nre.onre.constants.OnreExtractionPartType;
import edu.iitd.cse.open_nre.onre.constants.OnreFilePaths;
import edu.iitd.cse.open_nre.onre.domain.OnrePatternNode;
import edu.iitd.cse.open_nre.onre.domain.OnrePatternTree;
import edu.iitd.cse.open_nre.onre.helper.OnreHelper_json;
import edu.iitd.cse.open_nre.onre.utils.OnreIO;
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
		//System.out.println("I am running");
		//System.out.println(Onre_dsHelper.extractSIUnit("indian rupee"));
		String filePath_input = "/home/swarna/Desktop/nlp/project/ws/ONRE_DS/data/sentences.txt";
		List<String> stopWords = OnreIO.readFile(OnreFilePaths.filePath_stopWords);
		
		List<String> jsonDepTrees = OnreIO.readFile(filePath_input+OnreConstants.SUFFIX_JSON_STRINGS);
		
		Map<String, Set<Integer>> invertedIndex = (HashMap<String, Set<Integer>>)Onre_dsIO.readObjectFromFile(filePath_input+OnreConstants.SUFFIX_INVERTED_INDEX);
		
		List<Onre_dsFact> facts = Onre_dsHelper.readFacts(filePath_input+OnreConstants.SUFFIX_SEED_FACTS);
		
		List<String> patterns = new ArrayList<>();
		for (Onre_dsFact fact : facts) {
			Set<Integer> intersection = getSentenceIdsWithMentionedFact(invertedIndex, fact, stopWords);
			for (Integer id : intersection) {
				String jsonDepTree = jsonDepTrees.get(id);
				//if(jsonDepTree==null || jsonDepTree.equals("null")) continue;
				OnrePatternTree onrePatternTree = OnreHelper_json.getOnrePatternTree(jsonDepTree);
				String pattern = makePattern(onrePatternTree, fact);
				if(pattern != null && !patterns.contains(pattern)) patterns.add(pattern);
			}
		}
		
		OnreIO.writeFile(filePath_input+OnreConstants.SUFFIX_LEARNED_DEP_PATTERNS, patterns);
		System.out.println("----Done----");
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
	
	private static boolean quantityAndUnitSelection(OnrePatternNode qValueNode, OnrePatternNode qUnitNode) {
		List<OnrePatternNode> ancestors_qValue = getAncestors(qValueNode);
		List<OnrePatternNode> ancestors_qUnit = getAncestors(qUnitNode);
		OnrePatternNode intersectionNode = getIntersectionNode(ancestors_qValue, ancestors_qUnit);
		if(intersectionNode == null) return false; //no intersection node - invalid scenario
		
		int distance = 0;
		distance += getDistanceBetweenNodes(intersectionNode, qValueNode);
		distance += getDistanceBetweenNodes(intersectionNode, qUnitNode);
		
		if(distance > OnreConstants.MAX_DISTANCE_QUANTITY_UNIT) return false; //quantity and unit are far away - ignoring pattern
		
		OnrePatternNode nodeToBeUnvisited;
		//the node with more ancestors will be at lower level and thus shall be unvisited
		if(ancestors_qUnit.size()>ancestors_qValue.size()) nodeToBeUnvisited = qUnitNode;
		else nodeToBeUnvisited = qValueNode;
		markUnvisited(nodeToBeUnvisited);
		
		return true;
	}
	
	private static String makePattern(OnrePatternTree onrePatternTree, Onre_dsFact fact) {
		if(onrePatternTree == null) return null;
		OnrePatternNode argNode = searchNode_markVisited(onrePatternTree, fact.words[0], OnreExtractionPartType.ARGUMENT);
		OnrePatternNode relNode = searchNode_markVisited(onrePatternTree, fact.words[1], OnreExtractionPartType.RELATION);
		OnrePatternNode qValueNode = searchNode_markVisited(onrePatternTree, fact.words[2], OnreExtractionPartType.QUANTITY);
		OnrePatternNode qUnitNode = searchNode_markVisited(onrePatternTree, fact.words[3], OnreExtractionPartType.QUANTITY);
		
		if(!quantityAndUnitSelection(qValueNode, qUnitNode)) return null; //ignoring pattern
		
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
	
	private static void markUnvisited(OnrePatternNode node) {
		OnrePatternNode temp = node;
		while(temp!=null) {
			temp.visitedCount--;
			temp = temp.parent;
		}
	}
	
	private static void markVisited(OnrePatternNode node) {
		OnrePatternNode temp = node;
		while(temp!=null) {
			temp.visitedCount++;
			temp = temp.parent;
		}
	}
	
	private static int getDistanceBetweenNodes(OnrePatternNode higherLevelNode, OnrePatternNode lowerLevelNode) {
		int distance = 0;
		
		OnrePatternNode temp = lowerLevelNode;
		while(temp != higherLevelNode) {
			distance++; 
			if(temp==null) return Integer.MAX_VALUE; 
			temp=temp.parent;
		}
		
		return distance;
	}
	
	private static OnrePatternNode getIntersectionNode(List<OnrePatternNode> ancestors_qValue, List<OnrePatternNode> ancestors_qUnit) {
		int cntr = 0; 
		OnrePatternNode ancestor_qValue_temp = ancestors_qValue.get(cntr);
		OnrePatternNode ancestor_qUnit_temp = ancestors_qUnit.get(cntr);
		
		if(ancestor_qUnit_temp!=ancestor_qValue_temp) return null;
		
		++cntr;
		
		OnrePatternNode currentIntersectionNode = null;
		while(ancestor_qUnit_temp == ancestor_qValue_temp) {
			currentIntersectionNode = ancestor_qUnit_temp;
			if(cntr>=ancestors_qValue.size() || cntr>=ancestors_qUnit.size()) break;
			ancestor_qValue_temp = ancestors_qValue.get(cntr);
			ancestor_qUnit_temp = ancestors_qUnit.get(cntr);
			++cntr;
		}
		
		return currentIntersectionNode;
	}
	
	private static List<OnrePatternNode> getAncestors(OnrePatternNode node) {
		List<OnrePatternNode> ancestors = new ArrayList<>();
		OnrePatternNode temp = node;
		while(temp !=null) {
			ancestors.add(temp);
			temp = temp.parent;
		}
		
		Collections.reverse(ancestors);
		return ancestors;
	}
	
	private static OnrePatternNode searchNode_markVisited(OnrePatternTree onrePatternTree, String word, OnreExtractionPartType partType) {
		OnrePatternNode node = searchNode(onrePatternTree, word, partType);
		markVisited(node);
		node.nodeType = partType;
		return node;
	}
	
	//lowest node with visited count 3(visited by all three)
	private static OnrePatternNode findLCA(OnrePatternTree onrePatternTree) {
		OnrePatternNode root = onrePatternTree.root;
		
		OnrePatternNode LCA = null;
		
		Queue<OnrePatternNode> myQ = new LinkedList<>();
		myQ.add(root);
		
		while(!myQ.isEmpty()) {
			OnrePatternNode currNode = myQ.remove();
			if(currNode.visitedCount == 3) LCA = currNode;
			
			List<OnrePatternNode> children = currNode.children;
			for (OnrePatternNode child : children) {
				myQ.add(child);
			}
		}
		
		return LCA;
	}
}

