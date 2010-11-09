package ling;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Vector;

/*
 * The class Text - provide access to sentences
 * Can be used in incremental mode, in that case, it reads the sentence once at a time 
 * Otherwise stacks up the sentences in the member sentences
 */
public class Text {
	Vector<Sentence> sentences;
	BufferedReader dis = null;
	Vocabs vocab = null;
	String filename;
	LinkedList<Sentence> sentenceBuffer;

	/**
	 * Build a Text from a file name
	 * 
	 * @param filename
	 *            : the file
	 * @param v
	 *            : the vocab
	 * @param incremental
	 * @throws FileNotFoundException 
	 */
	public Text(String filename, Vocabs v) throws FileNotFoundException {
		this.vocab = v;
		this.filename = filename;
		sentenceBuffer = new LinkedList<Sentence>();

		try {
			dis = new BufferedReader(new InputStreamReader(new FileInputStream(
						filename), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Return True if there is more text available
	 **/
	public boolean isReady() {
		try {
			return dis.ready() || !sentenceBuffer.isEmpty();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Read a non empty sentence from disk
	 * @return a sentence or null if end of file
	 */
	
	public Sentence getNextSentence() {
		try {
			String line = new String();
			if (!dis.ready() && sentences != null)
				return null;
			while (dis.ready() && line.length() == 0) {
				line = dis.readLine();
				/* if we are not in incremental, we cannot skip empty sentences */
				if (sentences != null) break;
			}
			if (line.length() == 0 && sentences == null)
				return null;
			return new Sentence(vocab, line);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * rewind the buffer to the beginning of the file
	 */
	public void rewind() {
		try {
			dis.close();
			dis = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Vector<Sentence> sentences() {
		if(sentences == null) {
			sentences = new Vector<Sentence>();
			Sentence s;
			while((s = getNextSentence()) != null)
				sentences.add(s);
		}
		return sentences;
	}
	
	public Vocabs getVocabs() {
		return vocab;
	}
	
	public String toString() {
		StringBuffer o = new StringBuffer();
		for(Sentence s : sentences()) {
			o.append(s.toTokenString()+"\n");
		}
		return o.toString();
	}

}
