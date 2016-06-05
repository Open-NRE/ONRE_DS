/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import edu.iitd.cse.open_nre.onre.OnreGlobals;
import edu.iitd.cse.open_nre.onre.constants.OnreConstants;
import edu.iitd.cse.open_nre.onre.constants.OnreFilePaths;
import edu.iitd.cse.open_nre.onre.constants.Onre_dsRunType;
import edu.iitd.cse.open_nre.onre.domain.OnrePatternNode;
import edu.iitd.cse.open_nre.onre.domain.OnrePatternTree;
import edu.iitd.cse.open_nre.onre.domain.Onre_dsDanrothSpan;
import edu.iitd.cse.open_nre.onre.domain.Onre_dsDanrothSpans;
import edu.iitd.cse.open_nre.onre.helper.OnreHelper_DanrothQuantifier;
import edu.iitd.cse.open_nre.onre.helper.OnreHelper_json;
import edu.iitd.cse.open_nre.onre.utils.OnreIO;
import edu.iitd.cse.open_nre.onre.utils.OnreUtils;
import edu.iitd.cse.open_nre.onre.utils.OnreUtils_number;
import edu.iitd.cse.open_nre.onre.utils.OnreUtils_tree;
import edu.iitd.cse.open_nre.onre_ds.domain.Onre_dsFact;
import edu.iitd.cse.open_nre.onre_ds.helper.Onre_dsHelper;
import edu.iitd.cse.open_nre.onre_ds.helper.Onre_dsIO;


/**
 * @author harinder
 *
 */
public class Onre_dsRunMe {
	
	private static void setArguments(String[] args) {
		if(args.length>0) OnreGlobals.arg_runType = Onre_dsRunType.getType(args[0]);
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		Onre_dsRunMe.setArguments(args);

		File folder = new File(args[1]);
		
		Set<String> files = new TreeSet<>();
		OnreUtils.listFilesForFolder(folder, files);
		
		List<String> stopWords = OnreIO.readFile_classPath(OnreFilePaths.filePath_stopWords);
		List<Onre_dsFact> facts = Onre_dsHelper.readFacts("/home/harinder/Documents/IITD_MTP/Open_nre/ONRE/data/out_facts");
		
		Map<String, Integer> patternFrequencies = new HashMap<String, Integer>();
		for (String file : files) {
			
			if(!file.endsWith("_filtered")) continue;
			
			System.out.println("-------------------------running file: " + file);
			
			List<String> jsonDepTrees = OnreIO.readFile(file+OnreConstants.SUFFIX_JSON_STRINGS);
			Map<String, Set<Integer>> invertedIndex = (HashMap<String, Set<Integer>>)Onre_dsIO.readObjectFromFile(file+OnreConstants.SUFFIX_INVERTED_INDEX);
			
			List<Onre_dsDanrothSpans> listOfDanrothSpans = OnreHelper_DanrothQuantifier.getListOfDanrothSpans(file);
			
			//Map<String, Integer> patternFrequencies = new HashMap<String, Integer>();
			for (Onre_dsFact fact : facts) {
				
				if(!typeFilter(fact)) continue;
				
				//if(isType1or2() && fact.words.length == 3) continue; // Ignore the fact, if the fact has no unit
				//if(isType1or2() && fact.words[3].split(" ").length>1) continue; // Ignoring - unit has multiple words
				
				Set<Integer> intersection = Onre_dsRunMe.getSentenceIdsWithMentionedFact(invertedIndex, fact, stopWords);
				if(intersection == null) continue;
				
				for (Integer id : intersection) {
					String jsonDepTree = jsonDepTrees.get(id);
					//if(jsonDepTree==null || jsonDepTree.equals("null")) continue;
					OnrePatternTree onrePatternTree = OnreHelper_json.getOnrePatternTree(jsonDepTree);
					
					String pattern = Onre_dsRunMe.makePattern(onrePatternTree, fact, listOfDanrothSpans.get(id));
					if(pattern == null) continue;
					//System.out.println("patternLearned: sentence-"+onrePatternTree.sentence+", fact-"+fact+", pattern-"+pattern);
					
					if(patternFrequencies.containsKey(pattern)) {
						int count = patternFrequencies.get(pattern);
						patternFrequencies.put(pattern, count+1);
					}
					else patternFrequencies.put(pattern, 1);
				}
				
				//System.out.println(fact);
			}
		}
		patternFrequencies=OnreUtils.sortByValue(patternFrequencies);
		OnreIO.writeFileForMap(getOutFileName(), patternFrequencies);
		System.out.println("----Done----");
	}

	private static String getOutFileName() {
		return "data/out_learnedPatterns_"+OnreGlobals.arg_runType.text+"_"+OnreConstants.PARTIAL_VALUE_MATCHING_THRESOLD_PERCENT+"percent";
	}
	
	private static boolean typeFilter(Onre_dsFact fact) {
		
		switch (OnreGlobals.arg_runType) {
		case TYPE1:
			if(!hasUnit(fact)) return false;
			if(hasMultipleWords(fact)) return false;
			break;
			
		case TYPE2:
			if(!hasUnit(fact)) return false;
			if(hasMultipleWords(fact)) return false;
			break;
			
		case TYPE3:
			if(!hasUnit(fact)) return false;
			if(hasMultipleWords(fact)) return false;
			break;
			
		case TYPE4:
			if(hasMultipleWords(fact)) return false;
			break;
			
		case TYPE5:
			break;
		}
		
		return true;
	}

	private static boolean hasUnit(Onre_dsFact fact) {
		if(fact.words.length == 3) return false; // Ignore the fact, if the fact has no unit
		return true;
	}
	
	private static boolean hasMultipleWords(Onre_dsFact fact) {
		if(fact.words.length == 3) return false;
		if(fact.words[3].split(" ").length>1) return true; // Ignoring - unit has multiple words
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private static Set<Integer> getSentenceIdsWithMentionedFact(Map<String, Set<Integer>> invertedIndex, Onre_dsFact fact, List<String> stopWords) {
		
		List<Set<Integer>> listOfSetOfsentenceIds = new ArrayList<Set<Integer>>();
		
		for (String factWord : fact.words) {
			//factWord = factWord.toLowerCase();
			if(factWord.trim().isEmpty()) continue;
			if(stopWords.contains(factWord)) continue;
			
			if(!typeFilter(fact, factWord)) continue;
			
			TreeSet<Integer> setOfsentenceIds = (TreeSet<Integer>)(invertedIndex.get(factWord.trim()));
			if(setOfsentenceIds == null) return null;
			
			Set<Integer> setOfsentenceIds_cloned = (TreeSet<Integer>)setOfsentenceIds.clone();  
			if(setOfsentenceIds_cloned == null) return null;

			listOfSetOfsentenceIds.add(setOfsentenceIds_cloned);
		}
		
		Set<Integer> intersection = listOfSetOfsentenceIds.get(0);
		for (int i = 1; i < listOfSetOfsentenceIds.size(); i++) {
			intersection.retainAll(listOfSetOfsentenceIds.get(i));
		}
		
		return intersection;
	}

	private static boolean typeFilter(Onre_dsFact fact, String factWord) {
		if(isUnitType(fact, factWord)) return false; //don't match the qUnit
		
		switch(OnreGlobals.arg_runType){
		case TYPE1:
			if(isValueType(fact, factWord)) return false; //don't match the qValue
			break;
		case TYPE2:
			break;
		case TYPE3:
			if(isValueType(fact, factWord)) return false; //don't match the qValue
			break;
		case TYPE4:
			if(isValueType(fact, factWord)) return false; //don't match the qValue
			break;
		case TYPE5:
			if(isValueType(fact, factWord)) return false; //don't match the qValue
			break;
		}
		
		return true;
	}
	
	private static boolean isUnitType(Onre_dsFact fact, String factWord) {
		if(factWord.equals(fact.getQUnit())) return true; 
		return false;
	}

	private static boolean isValueType(Onre_dsFact fact, String factWord) {
		if(factWord.equals(fact.getQValue())) return true;
		return false;
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
	
	private static String makePattern(OnrePatternTree onrePatternTree, Onre_dsFact fact, Onre_dsDanrothSpans danrothSpans) {
		if(onrePatternTree == null) return null;
		
		OnreUtils_tree.sortPatternTree(onrePatternTree.root);

		OnrePatternNode argNode = searchNode_markVisited(onrePatternTree, fact.getArg());
		OnrePatternNode relNode = searchNode_markVisited(onrePatternTree, fact.getRel());
		if(argNode==null || relNode==null) return null;
		
		//OnrePatternNode qUnitNode = searchNode_markVisited(onrePatternTree, fact.getQUnit(), OnreExtractionPartType.QUANTITY);
		//if(qUnitNode==null) return null;
		
		OnrePatternNode qUnitNode=null, qValueNode=null;
		
		switch (OnreGlobals.arg_runType) {
		case TYPE1:
			qUnitNode = getQUnitNode(onrePatternTree, fact, danrothSpans);
			if(qUnitNode == null) return null;
			break;
			
		case TYPE2:
			qUnitNode = getQUnitNode(onrePatternTree, fact, danrothSpans);
			if(qUnitNode == null) return null;
			
			qValueNode = getQValueNode_matchAsString(onrePatternTree, fact);
			if(qValueNode==null) return null;

			break;

		case TYPE3:
			qUnitNode = getQUnitNode(onrePatternTree, fact, danrothSpans);
			if(qUnitNode == null) return null;
			
			qValueNode = getQValueNode_matchAsNumber(onrePatternTree, fact, danrothSpans);
			if(qValueNode==null) return null;
			break;
			
		case TYPE4:
			if(fact.getQUnit()!=null && !fact.getQUnit().equals("")) {
				qUnitNode = getQUnitNode(onrePatternTree, fact, danrothSpans);
				if(qUnitNode == null) return null;
			}
			
			qValueNode = getQValueNode_matchAsNumber(onrePatternTree, fact, danrothSpans);
			if(qValueNode==null) return null;
			break;
			
		case TYPE5:
			if(fact.getQUnit()!=null && !fact.getQUnit().equals("")) {
				qUnitNode = getQUnitNode(onrePatternTree, fact, danrothSpans);
				if(qUnitNode == null) return null;
			}
			
			qValueNode = getQValueNode_matchAsNumber(onrePatternTree, fact, danrothSpans);
			if(qValueNode==null) return null;
			break;
		}
		
		//need to select one whenever we have both unit and value
		if(qUnitNode!=null && qValueNode!=null) if(!quantityAndUnitSelection(qValueNode, qUnitNode)) return null; //ignoring pattern
		
		argNode.word = "{arg}";
		relNode.word = "{rel}";
		if(qValueNode!=null) qValueNode.word = "{quantity}";
		if(qUnitNode!=null) qUnitNode.word = "{quantity}";
		
		OnrePatternNode LCA = findLCA(onrePatternTree);
		StringBuilder sb_pattern = new StringBuilder();
		sb_pattern.append("<");
		makePattern_helper(LCA, sb_pattern);
		sb_pattern.append(">");
		String pattern = sb_pattern.toString();
		pattern = patternPostProcessing(pattern);
		return pattern;
	}

	private static OnrePatternNode getQValueNode_matchAsString(
			OnrePatternTree onrePatternTree, Onre_dsFact fact) {
		return searchNode_markVisited(onrePatternTree, fact.getQValue());
	}

	private static OnrePatternNode getQUnitNode(OnrePatternTree onrePatternTree, Onre_dsFact fact, Onre_dsDanrothSpans danrothSpans) {
		Map<String, String> map_quantifiers_unit = OnreHelper_DanrothQuantifier.getUnitMap(onrePatternTree.sentence, danrothSpans);
		String unitInPhrase = map_quantifiers_unit.get(fact.getQUnit());
		if(unitInPhrase == null) return null;
		
		if(unitInPhrase.split(" ").length>1 && OnreGlobals.arg_runType!=Onre_dsRunType.TYPE5) return null; //multipleWord units only allowed in type5 //this shall happen only in case of {percent, per cent}
		
		
		if(unitInPhrase.split(" ").length==1) return searchNode_markVisited(onrePatternTree, unitInPhrase);
		else return getQUnitNodeForMultiwordUnit(onrePatternTree, fact, danrothSpans, unitInPhrase);
	}

	private static OnrePatternNode getQUnitNodeForMultiwordUnit(OnrePatternTree onrePatternTree, Onre_dsFact fact, Onre_dsDanrothSpans danrothSpans, String unitInPhrase) {
		//---handling multiword units - will be used in case of type5---
		
		String []unitInPhrase_split = unitInPhrase.split(" ");
		List<OnrePatternNode> qUnitNodes = new ArrayList<>();
		
		Map<String, Onre_dsDanrothSpan> map_quantifiers_unitDanroth = OnreHelper_DanrothQuantifier.getUnitDanrothMap(onrePatternTree.sentence, danrothSpans);
		Onre_dsDanrothSpan danrothSpan = map_quantifiers_unitDanroth.get(fact.getQUnit());
		
		//finding the qUnitNodes
		for (String unitPart : unitInPhrase_split) {
			OnrePatternNode qUnitNode = searchNode(onrePatternTree, unitPart, danrothSpan);
			if(qUnitNode!=null) qUnitNodes.add(qUnitNode);
		}
		
		//getting the highest qUnitNode
		int topLevel=Integer.MAX_VALUE; OnrePatternNode highestQUnitNode=null;
		for (OnrePatternNode currQUnitNode : qUnitNodes) {
			if(currQUnitNode.level<topLevel) {topLevel=currQUnitNode.level; highestQUnitNode=currQUnitNode;}
		}
		
		markVisited(highestQUnitNode);
		return highestQUnitNode;
	}

	private static OnrePatternNode getQValueNode_matchAsNumber(OnrePatternTree onrePatternTree, Onre_dsFact fact, Onre_dsDanrothSpans danrothSpans) {
		
		OnrePatternNode qValueNode;
		Map<Double, String> map_quantifiers_value = OnreHelper_DanrothQuantifier.getValueMap(onrePatternTree.sentence, danrothSpans);

		Double closest = Double.MAX_VALUE;
		for(double key : map_quantifiers_value.keySet()) {
			double factQValue = fact.getQValue_double();
			double threshold = (OnreConstants.PARTIAL_VALUE_MATCHING_THRESOLD_PERCENT * factQValue)/100;
			double diff_abs_current = Math.abs(factQValue-key);
			double diff_abs_closest = Math.abs(factQValue-closest);
			if(diff_abs_current<=threshold && diff_abs_current<diff_abs_closest) closest=key;
		}
		
		String valueStr = map_quantifiers_value.get(closest);
		if(valueStr == null) return null; //value not found
		
		qValueNode = searchNode_markVisited(onrePatternTree, valueStr);
		
		if(qValueNode == null) {
			System.err.println("this shall never happen...exiting"); 
			System.exit(1);
		}
		
		return qValueNode;
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
		
		pattern = pattern.replaceAll("\\(prep#of#IN\\)", "(prep#of|for#IN)");
		pattern = pattern.replaceAll("\\(prep#for#IN\\)", "(prep#of|for#IN)");
		
		pattern = pattern.replaceAll("#\\{arg\\}#NNP\\|NN\\)", "#{arg}#NNP|NN|PRP)");
		//System.out.println(pattern);
		
		pattern = pattern.replaceFirst("#\\{quantity\\}#NNP\\|NN\\)", "#{quantity}#.+)");
		pattern = pattern.replaceFirst("#\\{quantity\\}#CD\\)", "#{quantity}#.+)");
		pattern = pattern.replaceFirst("#\\{quantity\\}#\\$\\)", "#{quantity}#.+)"); //TO-DO: not working
		pattern = pattern.replaceFirst("#\\{arg\\}#PRP\\$\\)", "#{arg}#PRP\\\\\\$)");
		
		pattern = pattern.trim().toLowerCase();
		if(!sanityCheck(pattern)) return null;
		
		return pattern;
	}
	
	private static boolean sanityCheck(String pattern) {
		if(!pattern.contains("{arg}")) return false;
		if(!pattern.contains("{rel}")) return false;
		if(!pattern.contains("{quantity}")) return false;
		return true;
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
	
	private static OnrePatternNode searchNode(OnrePatternTree onrePatternTree, String word) {
		OnrePatternNode root = onrePatternTree.root;
		
		Queue<OnrePatternNode> myQ = new LinkedList<>();
		myQ.add(root);
		
		while(!myQ.isEmpty()) {
			OnrePatternNode currNode = myQ.remove();
			
			if(nodeFound(word, currNode)) return currNode;
			
			List<OnrePatternNode> children = currNode.children;
			for (OnrePatternNode child : children) {
				myQ.add(child);
			}
		}
		
		//System.err.println("---It shall never come here...problem, exiting---");
		//System.exit(1); //TODO: this shall be uncommented..commented due to "\C2" special char issue
		return null;
	}
	
	private static OnrePatternNode searchNode(OnrePatternTree onrePatternTree, String word, Onre_dsDanrothSpan danrothSpan) {
		OnrePatternNode root = onrePatternTree.root;
		
		Queue<OnrePatternNode> myQ = new LinkedList<>();
		myQ.add(root);
		myQ.add(null);
		
		int level = 0;
		while(!myQ.isEmpty()) {
			OnrePatternNode currNode = myQ.remove();
			if(currNode==null && myQ.isEmpty()) break; 
			if(currNode==null) {level++; myQ.add(null); continue;}
			
			if(nodeFound(word, currNode, danrothSpan)) {currNode.level=level; return currNode;}
			
			List<OnrePatternNode> children = currNode.children;
			for (OnrePatternNode child : children) {
				myQ.add(child);
			}
		}
		
		//System.err.println("---It shall never come here...problem, exiting---");
		//System.exit(1); //TODO: this shall be uncommented..commented due to "\C2" special char issue
		return null;
	}
	
	private static boolean nodeFound(String word, OnrePatternNode currNode, Onre_dsDanrothSpan danrothSpan) {
		if(currNode.word.equalsIgnoreCase(word) && currNode.offset>=danrothSpan.start && currNode.offset<=danrothSpan.end) return true;
		
		/*//compare as a number
		try {
			if(OnreUtils_number.str2Double(currNode.word).equals(OnreUtils_number.str2Double(word))) return true;
		}catch(Exception e){
			//ignoring the exception--prob bcauz string can't be converted to a number
		}*/
		
		return false;
	}

	private static boolean nodeFound(String word, OnrePatternNode currNode) {
		if(currNode.word.equalsIgnoreCase(word)) return true;
		
		//compare as a number
		try {
			if(OnreUtils_number.str2Double(currNode.word).equals(OnreUtils_number.str2Double(word))) return true;
		}catch(Exception e){
			//ignoring the exception--prob bcauz string can't be converted to a number
		}
		
		return false;
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
	
	private static OnrePatternNode searchNode_markVisited(OnrePatternTree onrePatternTree, String word) {
		OnrePatternNode node = searchNode(onrePatternTree, word);
		if(node==null) return null;
		
		markVisited(node);
		//node.nodeType = partType;
		return node;
	}
	
	//lowest node with visited count 3(visited by all three factWords)
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

