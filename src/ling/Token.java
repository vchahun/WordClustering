package ling;

/**
 * @author senellart
 * Token class
 */
public class Token extends Cluster {
	Vocabs voc;
	
	public Token(String s, Vocabs v){
		super();
		id = v.getID(s.toLowerCase());
		voc = v;
	}
	public String toString(){
		return voc.getVocab(id);
	}

	public int getSize() {
		return voc.getVocab(id).length();
	}

}
