import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
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
		InputStream is = new FileInputStream("res/en-sent.bin");
		SentenceModel model = new SentenceModel(is);
		SentenceDetectorME sdetector = new SentenceDetectorME(model);

		String sentences[] = sdetector.sentDetect(paragraph);
		is.close();
		return sentences.length;
	}
	
	public static String[] Tokenize(String paragraph) throws InvalidFormatException, IOException 
	{	
		InputStream is = new FileInputStream("res/en-token.bin");
		TokenizerModel model = new TokenizerModel(is);
		Tokenizer tokenizer = new TokenizerME(model);
		String tokens[] = tokenizer.tokenize(paragraph);
	 
		is.close();
		return tokens;
	}
	
//	public static void parse(String sentence)
//	{
//		InputStream modelInParse = null;
//		try 
//		{
//			//load chunking model
//			modelInParse = new FileInputStream("en-parser-chunking.bin"); //from http://opennlp.sourceforge.net/models-1.5/
//			ParserModel model = new ParserModel(modelInParse);
//
//			//create parse tree
//			Parser parser = (Parser) ParserFactory.create(model);
//			Parse topParses[] = ParserTool.parseLine(sentence, parser, 1);
//
//			for (Parse p : topParses)
//				p.show();
//		}
//		catch (IOException e) 
//		{
//			e.printStackTrace();
//		}
//		finally {
//			if (modelInParse != null) 
//			{
//				try 
//				{
//					modelInParse.close();
//				}
//				catch (IOException e) 
//				{
//				}
//			}
//		}
//	}
	
	public static String[] generateTags(String[] words)
	{
		String[] tags = null;
		InputStream modelIn = null;

		try {
		  modelIn = new FileInputStream("res/en-pos-maxent.bin");
		  POSModel model = new POSModel(modelIn);
		  POSTaggerME tagger = new POSTaggerME(model);
		  tags = tagger.tag(words);
		}
		catch (IOException e) {
		  // Model loading failed, handle the error
		  e.printStackTrace();
		}
		finally {
		  if (modelIn != null) {
		    try {
		      modelIn.close();
		    }
		    catch (IOException e) {
		    }
		  }
		}
		
		
		return tags;
	}
	
	public static int countErrors(String[] tags)
	{
		int count = 0;
		
		for (int i = 0; i < tags.length-1; i++)
		{
			if((tags[i].equals("NN") && tags[i+1].equals("VBP"))
					|| (tags[i].equals("NNS") && tags[i+1].equals("VBP")))
				count++;
		}
		
		return count;
	}
	
	
	public static void main(String[] args) throws Exception
	{
		String text = "";
		try(BufferedReader br = new BufferedReader(new FileReader("essays/7965.txt"))) 
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
//		parse(text);
		String[] tags = generateTags(tokens);
		File dir = new File("spellchecker/");
		Directory directory = FSDirectory.open(dir);

		SpellChecker spellChecker = new SpellChecker(directory);

		spellChecker.indexDictionary(new PlainTextDictionary(new File("res/dictionary3.txt")));
		
//		for(String s : tags)
//			System.out.println(s + " ");
//		
		
		for(int i = 0; i<tags.length; i++)
		{
			System.out.println(tokens[i] + "|" + tags[i]);
		}
		int spellErrors = 0;
		for(int i = 0; i<tokens.length; i++)
		{
			//TODO Fix for two letter words, clitics, and punctuation
			if(tokens[i].length()>=3)
				if(!(spellChecker.exist(tokens[i].toLowerCase())))
				{
					//System.out.println("Couldn't find " + tokens[i]);
					spellErrors++;
				}
		}
		
		
		System.out.println("Spelling Errors: " + spellErrors);
		System.out.println("Verb agreement errors: " + countErrors(tags));
		spellChecker.close();

	}
}
