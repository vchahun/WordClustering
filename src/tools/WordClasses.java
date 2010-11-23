package tools;

import java.awt.Point;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import ling.Sentence;
import ling.Text;
import ling.Token;
import ling.Vocabs;

// TODO : replace i+N.j indexing by Point(i, j) for N big

class SparseMatrix<T> extends HashMap<Integer, T> {
	private static final long serialVersionUID = 1L;
	T defaultValue;
	
	public SparseMatrix(T v) {
		defaultValue = v;
	}
	
	public T get(Integer key) {
		T v = super.get(key);
		return v == null ? defaultValue : v;
	}
	
	public T put(Integer key, T value) {
		if(value.equals(defaultValue)) return defaultValue;
		return super.put(key, value);
	}
}

class Merge extends Point {
	private static final long serialVersionUID = 1L;
	public double DMI;
	public Merge(int x, int y, double v) {
		super(x, y);
		DMI = v;
	}
}

public class WordClasses {
	private Vocabs v;
	private final int N, S;
	
	private double logN;
	private double MI;
	
	private int[] count;
	private SparseMatrix<Integer> joint;
	private SparseMatrix<Double> Q;
	private double[] L;
	
	private ClusterTree tree;
	
	public WordClasses(Text text, int C) {
		v = text.getVocabs();
		Vector<Sentence> content = text.sentences();
		N = v.size(); logN = Math.log(N); S = C+1;
		System.out.println(N+" words found.");
		count = new int[N];
		joint = new SparseMatrix<Integer>(0);

		/* Compute co-occurence */
		for(Sentence s : content) {
			Vector<Token> tokens = s.getTokens();
			for(int i = 0; i < tokens.size()-1; i++)  {
				int x = tokens.get(i).getId(), y = tokens.get(i+1).getId();
				count[x]++; // count[x]++
				joint.put(x+N*y,joint.get(x+N*y)+1); // count[y|x]++
			}
		}
		System.out.println(joint.size()+" bigrams found.");
			
		Integer[] index = new Integer[N]; // index in sorted count
		for(int i = 0; i < N; i++)
			index[i] = i;

		// Sort count/joint by count
		Arrays.sort(index, new Comparator<Integer>() {   
			public int compare(Integer a, Integer b){
				return count[b]-count[a];
			}
		});
		
		int[] countS = new int[N], reverse = new int[N];
		System.out.println("Most frequent words:");
		for(int i = 0; i < N; i++) {
			countS[i] = count[index[i]];
			reverse[index[i]] = i;
			if(i<C) System.out.printf("%s(%d) ",v.getVocab(index[i]),countS[i]);
		}
		System.out.println();
		count = countS;
		
		SparseMatrix<Integer> jointS = new SparseMatrix<Integer>(0);
		for(int k : joint.keySet()) {
			int i = k%N, j = k/N;
			jointS.put(reverse[i]+N*reverse[j], joint.get(k));
		}
		joint = jointS;

		Q = new SparseMatrix<Double>(0.);
		computeMI();
		System.out.println("MI : "+MI/N);

		L = new double[S*S];
		computeL();
		
		tree = new ClusterTree(N);
		int free = C; // Free index in count
		
		for(int k = 0; k < N-C-1; k++) {
			System.out.print((100*(k+1))/(N-C-1)+"% | +"+v.getVocab(index[C+k])+" | ");
			int Ck = C+k; // (C+k+1)th element, decreasing count
			// Swap with a free slot
			count[free] = count[Ck]; count[Ck] = 0;
			for(int i = 0; i < N; i++) {
				joint.put(free+N*i, joint.get(Ck+N*i)); joint.remove(Ck+N*i);
				joint.put(i+N*free, joint.get(i+N*Ck)); joint.remove(i+N*Ck);
				Q.put(free+N*i, Q.get(Ck+N*i)); Q.remove(Ck+N*i);
				Q.put(i+N*free, Q.get(i+N*Ck)); Q.remove(i+N*Ck);
			}
			index[free] = index[Ck];
			for(int i = 0; i < S; i++) {
				L[free+S*i] = L(free, i);
				L[i+S*free] = L(i, free);
			}
			Merge merge = bestMerge(C+1);
			if(merge == null) {
				System.out.println("No possible merge after "+(k+1)+" steps");
				break;
			}
			else {
				int cx = index[merge.x], cy = index[merge.y];
				double DMI = merge.DMI;
				System.out.printf("Merge (%s, %s) | MI : %.3f + %.3f\n",v.getVocab(cx), v.getVocab(cy), MI, DMI);
				tree.merge(cx, cy, DMI);
				MI += DMI;
				System.err.println(MI);
				free = merge.y;
			}
		}
		System.out.println(tree.toString(v));
		
	}
	
	// Returns a.log(N.a/(b.c))
	private double nlogn(int a, int b, int c) {
		return a*b*c == 0 ? 0 : a * (logN + Math.log(a) - Math.log(b) - Math.log(c));
	}
	
	// Returns N * MI
	private void computeMI() {
		MI = 0;
		for(int k : joint.keySet()) {
			int i = k%N, j = k/N;
			int c12 = joint.get(k), c1 = count[i], c2 = count[j];
			double I = nlogn(c12, c1, c2); //c12*c1*c2 == 0 ? 0 : c12 * Math.log(N*c12/(c1*c2)); 
			MI += I; // c12 * log(N * c12 / (c1 * c2))
			Q.put(k, I);
		}
	}
	
	// O(S^3)
	private void computeL() {
		for(int i = 0; i < S-1; i++) {
			for(int j = 0; j < S-1; j++) {
				L[i+S*j] = L(i, j);
			}
		}
	}
	
	// O(S)
	private double L(int i, int j) {
		double Lij = -Q.get(i+N*i) -Q.get(j+N*j) + Q.get(i+N*j) + Q.get(j+N*i) + q22(i, j);
		for(int u = 0; u < S; u++) {
			// Compute L(i,j) = s(i+j) - s(i) - s(j)
			Lij += -Q.get(u+N*i) - Q.get(i+N*u) - Q.get(u+N*j) - Q.get(j+N*u) + q12(u, i, j) + q21(i, j, u);
		}
		return Lij;
	}
	
	private double q12(int l, int i, int j) { // Q.get(l, i+j)
		return nlogn(joint.get(l+N*i) + joint.get(l+N*j), count[i]+count[j], count[l]);
	}
	private double q21(int i, int j, int m) { // Q.get(i+j, m)
		return nlogn(joint.get(i+N*m) + joint.get(j+N*m), count[i]+count[j], count[m]);
	}
	private double q22(int i, int j) { // Q.get(i+j, i+j)
		int pijij =  joint.get(i+N*i) + joint.get(i+N*j) + joint.get(j+N*i) + joint.get(j+N*j);
		int pij = count[i]+count[j];
		return nlogn(pijij, pij, pij);
	}

	// Merges the clusters with the least DMI, returns false if no merge is possible
	private Merge bestMerge(int S) {
		double DMI = Double.MAX_VALUE; int x = -1, y = -1;
		for(int i = 0; i < S; i++) {
			if(count[i] == 0) continue;
			for(int j = i+1; j < S; j++) {
				if(count[j] == 0) continue;
				double Lij = L[i+S*j];
				if(Math.abs(Lij) < Math.abs(DMI)) {
					x = i; y = j;
					DMI = Lij;
				}
			}
		}
		if(x == -1)
			return null;
		
		int z = Math.max(x, y); x = Math.min(x, y); y = z; // x < y
		
		// Update L : remove terms for i and j
		for(int i = 0; i < S; i++) {
			for(int j = 0; j < S; j++) {
				L[i+S*j] += -q21(i, j, x) - q12(x, i, j) - q21(i, j, y) - q12(y, i, j);
			}
		}
		
		// Update count, joint & Q
		count[x] = count[x] + count[y]; count[y] = 0;
		for(int u = 0; u < N; u++) {
			if(u == x) {
				joint.put(x+N*x, joint.get(x+N*x)+joint.get(x+N*y)+joint.get(y+N*x)+joint.get(y+N*y)); // p(i+j,i+j)
				Q.put(x+N*x, q22(x, y)); // Q.get(i+j, i+j)
			}
			else if(u != y) {
				joint.put(u+N*x, joint.get(u+N*x)+joint.get(u+N*y));  // p(l, i+j)
				joint.put(x+N*u, joint.get(x+N*u)+joint.get(y+N*u)); // p(i+j, m)
				Q.put(x+N*u, q21(x, y, u)); // Q.get(l, i+j)
				Q.put(u+N*x, q12(u, x, y)); // Q.get(i+j, m)
			}
			 joint.remove(y+N*u); joint.remove(u+N*y);
			 Q.remove(y+N*u); Q.remove(u+N*y);
		}
		
		// Update L : add term for i+j
		for(int i = 0; i < S; i++) {
			for(int j = 0; j < S; j++) {
				L[i+S*j] += q21(i, j, x) + q12(x, i, j);
			}
		}
		
		// Update L(i,.) and L(.,i)
		for(int u = 0; u < S; u++) {
			L[x+S*u] = L(x, u); L[y+S*u] = 0;
			L[u+S*x] = L(u, x); L[u+S*y] = 0;
		}

		return new Merge(x, y, DMI);
	}

}
