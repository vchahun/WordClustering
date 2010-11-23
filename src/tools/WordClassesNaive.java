package tools;

import java.util.Vector;

import ling.Sentence;
import ling.Text;
import ling.Token;
import ling.Vocabs;

public class WordClassesNaive {
	private Vocabs v;
	private final int N;
	private int[] joint;
	private int[] count;
	private double[] Q;
	private double logN;
	private double MI;
	private ClusterTree tree;

	public WordClassesNaive(Text text, int C) {
		v = text.getVocabs();
		Vector<Sentence> content = text.sentences();
		N = v.size(); logN = Math.log(N);
		System.out.println(N+" words found.");
		joint = new int[N*N];
		count = new int[N];

		/* Compute co-occurence */
		for(Sentence s : content) {
			Vector<Token> tokens = s.getTokens();
			for(int i = 0; i < tokens.size()-1; i++)  {
				int x = tokens.get(i).getId(), y = tokens.get(i+1).getId();
				count[x]++; // count[x]++
				joint[x+N*y]++; // count[y|x]++
			}
		}

		computeMI();
		System.out.println("MI : "+MI/N);

		tree = new ClusterTree(N);

		for(int k = 0; k < N-C; k++) {
			System.out.print((100*(k+1))/(N-C)+"% | ");
			if(!bestMerge()) {
				System.out.println("No possible merge after "+(k+1)+" steps");
				break;
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
		Q = new double[N*N];
		for(int i = 0; i < N; i++) {
			for(int j = 0; j < N; j++) {
				int c12 = joint[i+N*j], c1 = count[i], c2 = count[j];
				double I = nlogn(c12, c1, c2); //c12*c1*c2 == 0 ? 0 : c12 * Math.log(N*c12/(c1*c2)); 
				MI += I; // c12 * log(N * c12 / (c1 * c2))
				Q[i+N*j] = I;
			}
		}
	}
	
	// Merges the clusters with the least DMI, returns false if no merge is possible
	private boolean bestMerge() {
		double DMI = Double.MAX_VALUE; int x = -1, y = -1;
		double[] Ql = null, Qm = null;
		for(int i = 0; i < N; i++) {
			if(count[i] == 0) continue;
			for(int j = i+1; j < N; j++) {
				if(count[j] == 0) continue;
				double[] Qlij = new double[N], Qijm = new double[N]; // q(.,i+j) and q(i+j,.)
				int pij = count[i]+count[j]; // p(i+j)
				int pijij = joint[i+N*i] + joint[i+N*j] + joint[j+N*i] + joint[j+N*j]; // p(i+j, i+j)
				double Qijij = nlogn(pijij, pij, pij); // q(i+j, i+j)
				double Lij = -Q[i+N*i] -Q[j+N*j] + Q[i+N*j] + Q[j+N*i] + Qijij;
				for(int u = 0; u < N; u++) {
					// Compute q(l, i+j) and q(i+j, m)
					int pijm = joint[i+N*u] + joint[j+N*u];
					Qijm[u] = nlogn(pijm, pij, count[u]);
					int plij = joint[u+N*i] + joint[u+N*j];
					Qlij[u] = nlogn(plij, pij, count[u]);
					// Compute L(i,j) = s(i+j) - s(i) - s(j)
					Lij += -Q[u+N*i] - Q[i+N*u] - Q[u+N*j] - Q[j+N*u] + Qlij[u] + Qijm[u];
				}

				if(Math.abs(Lij) < Math.abs(DMI)) {
					x = i; y = j;
					DMI = Lij;
					Ql = Qlij; Qm = Qijm;
					Ql[x] = Qijij; Qm[x] = Qijij;
				}
			}
		}
		if(x == -1)
			return false;
		System.out.printf("Merge (%s, %s) | DMI : %.3f ~> %.3f\n", v.getVocab(x), v.getVocab(y), DMI, (MI+DMI)/N);
		tree.merge(x, y, DMI);
		count[x] = count[x] + count[y]; count[y] = 0;
		for(int u = 0; u < N; u++) {
			joint[x+N*u] += joint[y+N*u]; joint[y+N*u] = 0;
			joint[u+N*x] += joint[u+N*y]; joint[u+N*y] = 0;
			Q[x+N*u] = Qm[u]; Q[y+N*u] = 0;
			Q[u+N*x] = Ql[u]; Q[u+N*y] = 0;
		}
		MI += DMI;
		return true;
	}
}
