/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import edu.iitd.cse.open_nre.onre.domain.OnrePatternNode;
import edu.iitd.cse.open_nre.onre.utils.OnreIO;
import edu.iitd.cse.open_nre.onre_ds.domain.Onre_dsFact;



/**
 * @author harinder
 *
 */
public class Onre_dsHelper {

	/*private static UnitExtractor unitExtractor;
	
	public static String extractSIUnit(String unitName) throws Exception {
		if(unitExtractor == null) unitExtractor = new UnitExtractor();
		
		Unit unit = unitExtractor.quantDict.getUnitFromBaseName(unitName);
		return unit.getParentQuantity().getCanonicalUnit().getBaseName();
	}*/
	
	/*public static DependencyGraph getDepGraph(String sentence) {
    	//sentence = preprocessing(sentence);
    	
	    //String cleaned = clean(sentence);
		ClearTokenizer tokenizer = new ClearTokenizer();
		ClearPostagger postagger = new ClearPostagger(tokenizer);
		DependencyParser parser = new ClearParser(postagger);
		
		DependencyGraph depGraph = null;
		
		try {
		depGraph = parser.apply(sentence);
		} catch(AssertionError error) {
			System.err.println("----->" + error.toString());
			return null;
		}
		
	    return depGraph;
    }*/
	
	public static List<Onre_dsFact> readFacts(String filePath_seedfacts) throws IOException {
		List<String> seedfacts = OnreIO.readFile(filePath_seedfacts);
		
		List<Onre_dsFact> facts = new ArrayList<>();
		for (String seedfact : seedfacts) {
			Onre_dsFact onre_dsFact = new Onre_dsFact();
			
			seedfact = seedfact.replaceAll("\\)$", "");
			seedfact = seedfact.replaceAll("^\\(", "");
			
			onre_dsFact.words = seedfact.split(" ; ");
			
			for (int i=0 ; i<onre_dsFact.words.length ; i++) {
				onre_dsFact.words[i] = onre_dsFact.words[i].toLowerCase();
				onre_dsFact.words[i] = onre_dsFact.words[i].replaceAll("\\.$", "");//TODO: no need of this after tokenizing
				onre_dsFact.words[i] = onre_dsFact.words[i].replaceAll("\\,$", "");//TODO: no need of this after tokenizing
			}
			
			facts.add(onre_dsFact);
		}
		
		return facts;
	}
	
	public static String getPatternNodeString(OnrePatternNode node) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(node.dependencyLabel);
		sb.append("#");
		sb.append(node.word);
		sb.append("#");
		sb.append(node.posTag);
		sb.append(")");
		return sb.toString();
	}
	
	
	static Tokenizer tokenizer;
	public static String[] tokenize(String str) throws InvalidFormatException, IOException {
		if(tokenizer == null) tokenizer = getTokenizer();
		return tokenizer.tokenize(str);
	}

	private static Tokenizer getTokenizer() throws IOException,
			InvalidFormatException {
		//InputStream modelIn = new FileInputStream("lib/en-token.bin");
		InputStream modelIn = Onre_dsHelper.class.getResourceAsStream("en-token.bin");
		TokenizerModel model = new TokenizerModel(modelIn);
		Tokenizer tokenizer = new TokenizerME(model);
		return tokenizer;
	}
	
}
