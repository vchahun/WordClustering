import java.io.FileNotFoundException;

import tools.WordClasses;

import ling.Text;
import ling.Vocabs;


public class Cluster {
	public static void main(String[] args) {
		if(args.length != 2) {
			System.err.println("Usage : java Cluster nClusters corpus.txt");
			System.exit(0);
		}
		Vocabs v = new Vocabs("French");
		try {
			Text t = new Text(args[1], v);
			new WordClasses(t, Integer.parseInt(args[0]));
		} catch (FileNotFoundException e) {
			System.err.println("Corpus file not found!");
		}
	}

}