package tools;

import ling.Token;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * The class Context is used to keep a limited text context
 * 
 * @author senellart
 * 
 */
public class Trie {
	Node root;

	public Trie() {
		root = new Node();
	}
	
	public int getTotalCount() {
		return root.getCount();
	}

	public class Node {
		public HashMap<Token, Node> transitions;
		int count, count_children;

		public Node() {
			transitions = new HashMap<Token, Node>();
			count = 0;
			count_children = 0;
		}
		public int getCount() { return count; }
		public int getCountChildren() { return count_children; }
		public String toString() {
			String S = new String();
			S="("+count+";";
			Set<Token> posibilities = transitions.keySet();
			for (Token t : posibilities) {
				S += t + "->" + transitions.get(t);
			}			
			S+=")";
			return S;
		}
	}

	public void addContext(Context c) {
		//System.err.println(c);
		LinkedList<Token> l = c.getContext();
		Node current = root;
		root.count++;
		for (Token t : l) {
			current.count_children++;
			if (!current.transitions.containsKey(t)) {
				current.transitions.put(t, new Node());
			}
			current = current.transitions.get(t);
			current.count++;
		}
	}

	/**
	 * @param c the context
	 * @return the node corresponding to the context or null
	 */
	public Node getNode(Context c) {
		LinkedList<Token> tokens = c.getContext();
		Node current = root;
		for (Token t : tokens) {
			if (current.transitions.containsKey(t)) {
				current = current.transitions.get(t);
			} else {
				return null;
			}
		}
		return current;
	}

	/**
	 * @param c the context
	 * @return the node corresponding to the context or null
	 */
	public Node getNode(Context c, Token t0) {
		LinkedList<Token> tokens = new LinkedList<Token>(c.getContext());
		tokens.addLast(t0);
		Node current = root;
		for (Token t : tokens) {
			if (current.transitions.containsKey(t)) {
				current = current.transitions.get(t);
			} else {
				return null;
			}
		}
		return current;
	}

	/**
	 * @param c
	 * @return select a node randomly
	 */
	public Token getRandom(Context c) {
		Node node = getNode(c);
		if (node != null) {
			double random = Math.random();
			Set<Token> posibilities = node.transitions.keySet();
			double d = 0;
			for (Token t : posibilities) {
				Node candidat = node.transitions.get(t);
				d = d + (((double) candidat.count) / node.count_children);
				if (d >= random) {
					return t;
				}
			}
		} else {
			try {
				c.forgetOne();
			} catch (Context.EmptyContext e) {}
			return getRandom(c);
		}
		return null;
	}
	
	public String toString() {
		return root.toString();
	}
}
