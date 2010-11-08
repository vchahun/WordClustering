package tools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import ling.Cluster;
import ling.Sentence;
import ling.Text;
import ling.Token;
import ling.Vocabs;

public class WordClasses {
	
	private HashMap<Cluster, Integer> joint;
	private HashMap<Cluster, Integer> count;
	private HashMap<Cluster, Double> Q;
	private Vocabs v;
	private final int N;
	private final double logN;
	
	public WordClasses(Text text, int C) {
		joint = new HashMap<Cluster, Integer>();
		count = new HashMap<Cluster, Integer>();
		v = text.getVocabs();
		/* Compute co-occurence */
		for(Sentence s : text.sentences()) {
			Vector<Token> tokens = s.getTokens();
			for(int i = 0; i < tokens.size()-1; i++)  {
				Token x = tokens.get(i), y = tokens.get(i+1);
				if(!count.containsKey(x)) 
					count.put(x, 0);
				Cluster xy = new Cluster(x, y, v);
				joint.put(xy, cooccurrence(x, y) + 1); // count[y|x]++
				count.put(x, occurrence(x) + 1); // count[x]++
			}
		}
		N = count.size();
		logN = Math.log(N);
		System.out.println(N+" words and "+joint.size()+" bigrams found.");
		/* Remove edge words */
		Iterator<Cluster> it = joint.keySet().iterator();
		int e = 0;
		while(it.hasNext()) {			
			if(!count.containsKey(it.next().y)) {
				it.remove();
				e++;
			}
		}
		System.out.println(e+" edge words removed.");
		
		/* Initial computation */
		Q = new HashMap<Cluster, Double>();
		final double MI = computeMI();
		System.out.println("MI : "+MI/N);
		
		// Compute sk(i)
		HashMap<Cluster, Double> S = new HashMap<Cluster, Double>();
		for(Cluster c : count.keySet()) {
			S.put(c, -q(c, c));
		}
		for(Cluster p : joint.keySet()) {
			S.put(p.x, S.get(p.x) + Q.get(p)); // qk(i, m)
			S.put(p.y, S.get(p.y) + Q.get(p)); // qk(l, i)
		}
		
		// Compute L(i,j)
		HashMap<Cluster, Double> L = new HashMap<Cluster, Double>();
		for(Cluster p : joint.keySet()) {
			double v = S.get(p.x) + S.get(p.y) - Q.get(p) - q(p.y, p.x); // sk(i) + sk(j) - qk(i, j) - qk(j, i) [- qk(i+j,i+j)]
			L.put(p, v);
		}
		
		System.out.println("Begin computing V3");
		
		 // Compute qk(i+j, m)
		for(Cluster p : joint.keySet()) {
			double logPl = Math.log(count.get(p.x) + count.get(p.y));
			for(Cluster c : count.keySet()) {
				// (14)
				double pc = cooccurrence(p.x, c) + cooccurrence(p.y, c);
				double pr = count.get(c);
				double qpc = pc == 0 ? 0 : pc*(logN + Math.log(pc) - logPl - Math.log(pr));
				pc = cooccurrence(c, p.x) + cooccurrence(c, p.y);
				double qcp = pc == 0 ? 0 : pc*(logN + Math.log(pc) - logPl - Math.log(pr));
				// (15)
				if(!c.equals(p.x) && !c.equals(p.x)) {
					L.put(p, L.get(p) - qcp - qpc); // -qk(l, i+j) - qk(i+j, m)
				}
			}
		}
		
		// THE loop!
		for(int i = 0; i < C; i++) {
			System.out.println("The loop #"+i);
			double minV = Double.MAX_VALUE;
			Cluster minP = null;
			for(Cluster p : joint.keySet()) {
				if(!p.x.equals(p.y)) {
					double l = Math.abs(L.get(p));
					if(l < minV) {
						minV = l;
						minP = p;
					}
				}
			}
			if(minP == null) {
				System.err.println("No more merge possible.");
				break;
			}
			System.out.println("Merge "+minP+" | DMI : "+minV);
			// Update values
			// Remove i,j / Insert i+j
			for(Cluster c : count.keySet()) {
				if(!c.equals(minP.x) && !c.equals(minP.y)) {
					Integer xc = joint.remove(new Cluster(minP.x, c, v)), yc = joint.remove(new Cluster(minP.y, c, v));
					joint.put(new Cluster(minP, c, v), (xc == null ? 0 : xc) + (yc == null ? 0 : yc));
					Integer cx = joint.remove(new Cluster(c, minP.x, v)), cy = joint.remove(new Cluster(c, minP.y, v));
					joint.put(new Cluster(c, minP, v), (cx == null ? 0 : cx) + (cy == null ? 0 : cy));
				}
				else {
					joint.remove(new Cluster(c, minP.x, v)); joint.remove(new Cluster(c, minP.y, v));
					joint.remove(new Cluster(c, c, v));
				}
			}
			int countP = count.remove(minP.x) + count.remove(minP.y);
			count.put(minP, countP);
			// Create Q(k-1)
			double logPl = Math.log(countP);
			for(Cluster c : count.keySet()) {
				// (14)
				double pc = cooccurrence(minP, c);
				double pr = count.get(c);
				double qpc = pc == 0 ? 0 : pc*(logN + Math.log(pc) - logPl - Math.log(pr));
				pc = cooccurrence(c, minP);
				double qcp = pc == 0 ? 0 : pc*(logN + Math.log(pc) - logPl - Math.log(pr));
				Q.put(new Cluster(minP, c, v), qpc);
				Q.put(new Cluster(c, minP, v), qcp);
			}
			// Update sk(i)
			for(Cluster c : count.keySet()) {
				if(c.equals(minP))
					continue; // sk(i+j)
				double d = S.get(c)
				- q(c, minP.x) - q(minP.x, c)
				- q(c, minP.y) - q(minP.y, c)
				+ q(c, minP) + q(minP, c);
				S.put(c, d); // sk(l) - qk(l, i) - qk(i, l) - qk(l,j) - qk(j,l) + q(k-1)(l, i) + q(k-1)(i, l)
			}
			// Update Lk(i,j)
			logPl = Math.log(countP);
			for(Cluster p : joint.keySet()) {
				if(p.x.equals(minP) || p.y.equals(minP))
					continue; // Lk(i+j, l)
				double pc = cooccurrence(p.x, minP) +  cooccurrence(p.y, minP);
				double pr = count.get(p.x) + count.get(p.y);
				double qpc =  pc == 0 ? 0 : pc*(logN + Math.log(pc) - logPl - Math.log(pr));
				pc = cooccurrence(minP, p.x) +  cooccurrence(minP, p.y);
				double qcp = pc == 0 ? 0 : pc*(logN + Math.log(pc) - logPl - Math.log(pr));
				double d = L.get(p)
				- q(p, minP.x) - q(minP.x, p)
				- q(p, minP.y) - q(minP.y, p)
				- qpc - qcp;
				L.put(p, d); // Lk(l,m) - qk(l+m, i) - qk(i, l+m) - qk(l+m, j) - qk(j, l+m) + q(k-1)(l+m, i) + q(k-1)(i, l+m)
			}
			// Insert S(i+j), L(i+j, l)
			double sij = - q(minP, minP); // qk(i+j, i+j)
			for(Cluster p : joint.keySet()) {
				sij += q(p.x, minP) + q(minP, p.y); // qk(l, i+j) + qk(i+j, m)
			}
			S.put(minP, sij);
			for(Cluster c : count.keySet()) {
				double l = sij + S.get(c) - q(c, minP) - q(minP, c);
				L.put(new Cluster(c, minP, v), l); // sk(l) + sk(i+j) - q(k-1)(l, i) - q(k-1)(i, l) [- q(k-1)(i+l, i+l)]
				L.put(new Cluster(minP, c, v), l);
			}
			for(Cluster p : joint.keySet()) {
				// (14)
				double pc = cooccurrence(p.x, p.y) + cooccurrence(p.x, minP);
				double pl = count.get(p.x), pr = cooccurrence(p.y, minP);
				double qpc = pc == 0 ? 0 : pc*(logN + Math.log(pc) - Math.log(pl) - Math.log(pr));
				pc = cooccurrence(p.y, p.x) + cooccurrence(minP, p.x);
				double qcp = pc == 0 ? 0 : pc*(logN + Math.log(pc) - Math.log(pl) - Math.log(pr));
				// (15)
				if(!p.x.equals(p.y) && !p.x.equals(minP)) {
					Cluster lij = new Cluster(p.x, minP, v);
					L.put(lij, L.get(lij) - qcp - qpc); // -q(k-1)(l, m+i) - q(k-1)(l+i, m)
				}
				if(!p.y.equals(p.x) && !p.y.equals(minP)) {
					Cluster ijm = new Cluster(minP, p.y, v);
					L.put(ijm, L.get(ijm) - qcp - qpc);
				}
			}

		/*	for(Cluster c : count.keySet()) {
				joint.remove(new Cluster(minP, c, v));
				joint.remove(new Cluster(c, minP, v));
			}
			count.remove(minP); */
		}
		
	}
	
	private int occurrence(Cluster c) {
		Integer cc = count.get(c);
		return cc == null ? 0 : cc;
	}
	
	private int cooccurrence(Cluster x, Cluster y) {
		Integer c12 = joint.get(new Cluster(x, y, v));
		return c12 == null ? 0 : c12;
	}

	private double q(Cluster x, Cluster y) {
		Double c12 = Q.get(new Cluster(x, y, v));
		return c12 == null ? 0. : c12;
	}
	
	// Returns N * MI
	private double computeMI() {
		double MI = 0;
		HashMap<Cluster, Double> logCount = new HashMap<Cluster, Double>();
		for(Cluster c : count.keySet())
			logCount.put(c, Math.log(count.get(c)));
		for(Cluster p : joint.keySet()) {
				int c12 = joint.get(p);
				double logc1 = logCount.get(p.x), logc2 = logCount.get(p.y);
				double I = c12 == 0 ? 0 : c12 * (logN + Math.log(c12) - (logc1 + logc2)); // c12 * log(N * c12 / (c1 * c2))
					MI += I;
					Q.put(p, I);
		}
		return MI;
	}
}
