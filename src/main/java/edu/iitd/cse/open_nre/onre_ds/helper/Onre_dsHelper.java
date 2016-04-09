/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.helper;

import catalog.Unit;
import eval.UnitExtractor;

/**
 * @author harinder
 *
 */
public class Onre_dsHelper {

	private static UnitExtractor unitExtractor;
	
	public static String extractSIUnit(String unitName) throws Exception {
		if(unitExtractor == null) unitExtractor = new UnitExtractor();
		
		Unit unit = unitExtractor.quantDict.getUnitFromBaseName(unitName);
		return unit.getParentQuantity().getCanonicalUnit().getBaseName();
	}
}
