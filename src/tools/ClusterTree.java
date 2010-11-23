package tools;

import ling.Vocabs;

class ClusterNode {
	public int id = -1;
	ClusterNode x, y;
	public double DMI;
	public ClusterNode(ClusterNode x, ClusterNode y, double DMI) {
		this.x = x;
		this.y = y;
		this.DMI = x.DMI + y.DMI + DMI;
	}
	public ClusterNode(int id) {
		this.id = id;
		DMI = 0;
	}
	public String toString(Vocabs v) {
		return id == -1 ? '('+this.x.toString(v)+','+this.y.toString(v)+')' : v.getVocab(id);
	}
}

public class ClusterTree {

	private ClusterNode[] nodes;
	
	public ClusterTree(int N) {
		nodes = new ClusterNode[N];
		for(int i = 0; i < N; i++)
			nodes[i] = new ClusterNode(i);
	}
	
	public String toString(Vocabs v) {
		StringBuffer s = new StringBuffer();
		for(ClusterNode c : nodes) {
			if(c != null && c.id == -1)
				s.append(c.DMI+" | "+c.toString(v)+"\n");
		}
		return s.toString();
	}
	
	public void merge(int x, int y, double DMI) {
		nodes[x] = new ClusterNode(nodes[x], nodes[y], DMI);
		nodes[y] = null;
	}
	
}
