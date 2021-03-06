/**
 * 
 */
package edu.iitd.cse.open_nre.onre_ds.domain;


/**
 * @author harinder
 *
 */
public class Onre_dsFact {
	
/*	public String arg;
	public String rel;
	public String q_value;
	public String q_unit;*/
	public String[] words = new String[5];
	
	public String toString() {
		String temp = "(";
		for(int i=0; i<this.words.length; i++)
		{
			temp = temp + this.words[i];
			if(i<this.words.length-1) temp = temp + " ; ";
			else temp = temp + ")";
		}
		return temp;
	}
	
	public String getArg() {
		return this.words[0];
	}
	
	public String getRel() {
		return this.words[1];
	}
	
	public String getQValue() {
		return this.words[2];
	}
	
	public double getQValue_double() {
		return Double.valueOf(this.words[2]);
	}
	
	public String getQUnit() {
		if(this.words.length==3) return null; //qUnit not present
		return this.words[3];
	}
	
	public String getFullRelation() {
		return this.words[4];
	}
}
