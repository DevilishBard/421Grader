import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class AutoGrader
{
	
	public static int SentenceDetect(String paragraph) throws InvalidFormatException, IOException 
	{
		InputStream is = new FileInputStream("en-sent.bin");
		SentenceModel model = new SentenceModel(is);
		SentenceDetectorME sdetector = new SentenceDetectorME(model);

		String sentences[] = sdetector.sentDetect(paragraph);
		is.close();
		return sentences.length;
	}
	
	public static String[] Tokenize(String paragraph) throws InvalidFormatException, IOException 
	{	
		InputStream is = new FileInputStream("en-token.bin");
		TokenizerModel model = new TokenizerModel(is);
		Tokenizer tokenizer = new TokenizerME(model);
		String tokens[] = tokenizer.tokenize(paragraph);
	 
		is.close();
		return tokens;
	}
	
	public static void main(String[] args) throws Exception
	{
		String text = "";
		try(BufferedReader br = new BufferedReader(new FileReader("11580.txt"))) 
		{
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) 
	        {
	            sb.append(line);
	            sb.append(System.lineSeparator());
	            line = br.readLine();
	        }
	        text = sb.toString();
	    }
		
		System.out.println("Sentence Count: " + SentenceDetect(text));
		String tokens[] = Tokenize(text);
		
		File dir = new File("spellchecker/spellchecker");
		Directory directory = FSDirectory.open(dir);

		SpellChecker spellChecker = new SpellChecker(directory);

		spellChecker.indexDictionary(new PlainTextDictionary(new File("421Grader/dictionary3.txt")));
		

		int spellErrors = 0;
		for(int i = 0; i<tokens.length; i++)
		{
			//TODO Fix for two letter words, clitics, and punctuation
			if(tokens[i].length()>=3)
				if(!(spellChecker.exist(tokens[i].toLowerCase())))
				{
					System.out.println("Couldn't find " + tokens[i]);
					spellErrors++;
				}
		}
		
		
		System.out.println("Spelling Errors: " + spellErrors);
		
		spellChecker.close();

	}
}
