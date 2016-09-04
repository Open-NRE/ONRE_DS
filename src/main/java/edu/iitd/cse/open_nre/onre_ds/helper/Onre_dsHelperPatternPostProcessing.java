/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.helper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.iitd.cse.open_nre.onre.constants.OnreFilePaths;
import edu.iitd.cse.open_nre.onre.domain.OnrePatternNode;
import edu.iitd.cse.open_nre.onre.helper.OnreHelper_WordNet;
import edu.iitd.cse.open_nre.onre.helper.OnreHelper_pattern;
import edu.iitd.cse.open_nre.onre.utils.OnreIO;

/**
 * @author swarna
 *
 */
public class Onre_dsHelperPatternPostProcessing {
	
	public static String patternPostProcessing(String pattern) throws IOException {
		pattern = basicReplacements(pattern);
		if(!sanityCheck(pattern)) return null;
		//pattern = replaceWithInflectedWords(pattern, stemmedWordToUnstemmedWordsMap);
		return pattern;
	}
	
	private static String basicReplacements(String pattern) {
		//TODO: this function is as of now as per facts#0...need to be modified --- IMPORTANT
		
		String str_isAndOthers = "#is|are|was|were|been|be#";
		String str_hasAndOthers = "#has|have|had|having#";
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
		pattern = pattern.replaceAll("#PRP\\)", str_noun);
		//System.out.println(pattern);
		
		//pattern = pattern.replaceAll("\\(prep#of#IN\\)", "(prep#of|for#IN)");
		//pattern = pattern.replaceAll("\\(prep#for#IN\\)", "(prep#of|for#IN)");
		
		pattern = pattern.replaceAll("#\\{arg\\}#NNP\\|NN\\)", "#{arg}#NNP|NN|PRP)");
		//pattern = pattern.replaceAll("#\\{arg\\}#PRP)", "#{arg}#NNP|NN|PRP)");//TODO: commented for now
		//System.out.println(pattern);
		
		pattern = pattern.replaceFirst("#\\{quantity\\}#NNP\\|NN\\)", "#{quantity}#.+)");
		pattern = pattern.replaceFirst("#\\{quantity\\}#CD\\)", "#{quantity}#.+)");
		pattern = pattern.replaceFirst("#\\{quantity\\}#\\$\\)", "#{quantity}#.+)"); //TO-DO: not working
		pattern = pattern.replaceFirst("#\\{arg\\}#PRP\\$\\)", "#{arg}#PRP\\\\\\$)");
		
		pattern = pattern.trim().toLowerCase();
		
		return pattern;
	}
	
	private static boolean sanityCheck(String pattern) {
		if(!pattern.contains("{arg}")) return false;
		if(!pattern.contains("{rel}")) return false;
		if(!pattern.contains("{quantity}")) return false;
		if(!pattern.contains("{arg}#nnp|nn|prp")) return false; // argument has to be a noun/pronoun
		/*if(pattern.contains("{rel}#rb")) return false; // patterns with adverb relations are ignored
		if(pattern.contains("conj")) return false; // remove pattern with conjunction words
*/		return true;
	}
}
