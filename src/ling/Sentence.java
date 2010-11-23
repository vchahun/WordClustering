package ling;
import java.util.Vector;

/**
 * @author senellart
 * 
 * The Sentence class - breaks a sentence into a token vector 
 */
public class Sentence {
	Vector<Token> tokens;
	Vocabs vocabs;
	String repr;

	public Sentence(Vocabs TheVocab, String s) {
		vocabs = TheVocab;
		tokens = parse(s);
		repr = s;
	}
	
	public Sentence(Vocabs v, Vector<Token> t) {
		vocabs = v;
		tokens = t;
	}

	static int getType(char c) {
		/* space or control-character */
		if (Character.isSpaceChar(c) || Character.isISOControl(c))
			return 0;
		/* letter */
		if (Character.isLetter(c))
			return 1;
		/* digit */
		if (Character.isDigit(c))
			return 2;
		return 3;
	}

	Vector<Token> parse(String s) {
		Vector<Token> Mytokens = new Vector<Token>();
		int size = s.length();
		if (size == 0) return Mytokens;
		/* current character */
		int i = 1;
		/* character corresponding to the beginning of the region */
		int last = 0;
		/* type of the current region */
		int curType = getType(s.charAt(0));
		/* parse the string */
		while (i < size) {
			int Type = getType(s.charAt(i));
			if (Type != curType || curType == 3) {
				/* if previous character is not a space */
				if (curType != 0 && curType != 3) {
					Mytokens.add(new Token(s.substring(last, i), vocabs));
				}
				last = i;
				curType = Type;
			}
			i++;
		}
		if (curType != 0 && curType != 3)
		Mytokens.add(new Token(s.substring(last, size), vocabs));
		return Mytokens;
	}

	public String toString() {
		return repr;
	}

	public String toTokenString() {
		String r = new String();
		for (Token t : tokens) {
			r = r + "{"+t.toString()+"}";
		}
		return r;
	}
	
	public int getSize() {
		int n = 0;
		for(Token t : tokens) {
			n += t.getSize();
		}
		return n;
	}
	
	public Vector<Token> getTokens() {
		return tokens;
	}
}
