package ling;

import java.util.HashMap;
import java.util.Vector;

/**
 * @author senellart
 * Vocabulary class
 * Provide token>id and id>token mapping
 */
public class Vocabs {
	/*
	 * Hash Vocab>ID
	 */
	private HashMap<String,Integer> HashVoc ;
	/*
	 * Vector of Vocabs: ID>Vocab
	 */
	Vector<String> VectorVoc;
	String lang;
	
	public Vocabs(String language){
		VectorVoc=new Vector<String>();
		HashVoc=new HashMap<String,Integer>();
		lang=language;
	}
	
	public boolean contains(String s){
		if (HashVoc.containsKey(s) )return true;
		return false;
	}
	public int getID(String w){
		if(contains(w)){
			return HashVoc.get(w);
		}
		else{
			VectorVoc.add(w);
			HashVoc.put(w,VectorVoc.size()-1);
			return HashVoc.size()-1;
		}
		
	}
	public String getVocab(int id){
		return VectorVoc.get(id);
	}
	
	public int size() {
		return VectorVoc.size();
	}
	
}
