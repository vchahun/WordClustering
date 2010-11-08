package tools;

import java.util.LinkedList;

import ling.*;

/**
 * The class Context is used to keep a limited text context
 * 
 * @author senellart
 * 
 */
public class Context {

	public class EmptyContext extends Exception {
		private static final long serialVersionUID = 1L;

		public String toString() {
			return "{}";
		}
	}

	private int order;
	private LinkedList<Token> history = null;

	/**
	 * Build a context
	 * 
	 * @param theorder
	 */
	public Context(int theorder) {
		order = theorder;
		history = new LinkedList<Token>();
	}

	public Context(Context c) {
		order = c.order;
		history = new LinkedList<Token>();
		for (Token t : c.history)
			history.addLast(t);
	}	
	
	public Context(Vocabs v, String e) {
		Sentence S = new Sentence(v,e);
		order = S.getTokens().size();
		history = new LinkedList<Token>();
		for (Token t : S.getTokens())
			history.addLast(t);
	}

	/**
	 * keep a fix size history
	 */
	public void addToken(Token t) {
		history.addLast(t);
		if (history.size() > order) {
			history.removeFirst();
		}
	}

	/**
	 * Print the context
	 */
	public String toString() {
		String s = "{";
		for (Token t : history) {
			if (t != null) {
				s += t;
			}
		}
		s += "}";
		return s;
	}

	/**
	 * return the size of the current context
	 */
	public int size() {
		return history.size();
	}

	/**
	 * checks if the context is empty
	 */
	public boolean isEmpty() {
		return history.size() == 0;
	}

	/**
	 * Return the order of the context
	 */
	public int order() {
		return order;
	}

	/**
	 * Returns the context
	 * 
	 * @return linked list
	 */
	public LinkedList<Token> getContext() {
		return history;
	}

	/**
	 * truncate the context to a given size
	 * 
	 * @param size
	 */
	public void truncate(int size) {
		while (size() > size) {
			history.removeFirst();
		}
	}

	/**
	 * forget one token from the context
	 * @return 
	 */
	public Token forgetOne() throws EmptyContext {
		if (size() == 0) {
			throw new EmptyContext();
		} else
			return history.removeFirst();
	}
}
