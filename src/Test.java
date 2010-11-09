import java.io.FileNotFoundException;

import tools.WordClasses;

import ling.Text;
import ling.Vocabs;


public class Test {
	public static void main(String[] args) {
		Vocabs v = new Vocabs("French");
		try {
			Text t = new Text("./corpus/lemonde_10.txt", v);
			new WordClasses(t, 1000);
		} catch (FileNotFoundException e) {
			System.err.println("Corpus file not found!");
		}
	}

}