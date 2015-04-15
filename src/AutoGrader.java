import java.io.File;
import java.io.FileInputStream;
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
		
		// always start with a model, a model is learned from training data
		InputStream is = new FileInputStream("en-sent.bin");
		SentenceModel model = new SentenceModel(is);
		SentenceDetectorME sdetector = new SentenceDetectorME(model);

		String sentences[] = sdetector.sentDetect(paragraph);
		is.close();
		return sentences.length;
		
	}
	
	public static String[] Tokenize(String paragraph) throws InvalidFormatException, IOException {
		InputStream is = new FileInputStream("en-token.bin");
	 
		TokenizerModel model = new TokenizerModel(is);
	 
		Tokenizer tokenizer = new TokenizerME(model);
	 
		String tokens[] = tokenizer.tokenize(paragraph);
	 
		is.close();
		return tokens;
	}
	
	public static void main(String[] args) throws Exception
	{
		String paragraph = "How are you? This is Mike. Blah blah blah.";
		System.out.println("Sentence Count: " + SentenceDetect(paragraph));
		String tokens[] = Tokenize(paragraph);
		
		File dir = new File("c:/spellchecker/");
		Directory directory = FSDirectory.open(dir);

		SpellChecker spellChecker = new SpellChecker(directory);

		spellChecker.indexDictionary(new PlainTextDictionary(new File("421Grader/fulldictionary00.txt")));
		
		int spellErrors = 0;
		for(int i = 0; i<tokens.length; i++)
		{
			if(!spellChecker.exist(tokens[i].toLowerCase()))
			{
				System.out.println("Couldn't find " + tokens[i]);
				spellErrors++;
			}
		}
		
		
		System.out.println("Spelling Errors: " + spellErrors);
		
		spellChecker.close();

	}
}
