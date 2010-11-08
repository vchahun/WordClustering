package ling;

public class Cluster implements Comparable<Cluster> {	
	int id;
	public Cluster x, y;
	
	public Cluster() {}

	public Cluster(Cluster x, Cluster y, Vocabs v) {
		this.x = x;
		this.y = y;
		id = v.getID(x.getId()+"+"+y.getId());
	}
	
	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (this == null || other == null) return false;
		Cluster o = (Cluster)other;
		return (o.id == id);
	}

	@Override
	public int compareTo(Cluster t) {
		if (id < t.id) return -1;
		if (id == t.id) return 0;
		return 1;
	}

	public int getId() {
		return id;
	}

	public String toString() {
		return "("+x+"+"+y+")";
	}
}
