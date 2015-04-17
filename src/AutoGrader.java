import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import java.io.PrintStream;

import java.util.List;


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
//import org.languagetool.*;
//import org.languagetool.language.AmericanEnglish;
//import org.languagetool.rules.RuleMatch;


/*
 * I had to comment out the code you wrote and the import statements to get rid of some errors
 * because the libraries you added aren't being tracked by git. Can you just comment your changes 
 * back in, then right click on the project name then go to 'team' and click on 'add to index'
 * that should have git track the new libraries. Then if you commit that back everything should be 
 * fine. Also, you may have to change the build path back to include those new jars. Sorry.
 */

public class AutoGrader
{
	// Temporary path to identify which folder to check for input
	private static String inputPath = "input/test/original/low/";
	// The number of subject-verb agreement errors per essay
	private static int subVerbErrors = 0;
	// The number of verb-tense, missing verb, and extra verb errors per essay
	private static int verbTenseErrors = 0;
	
	private static int spellingScore  = 0;
	private static int subVerbScore   = 0;
	private static int verbTenseScore = 0;
	private static int sentFormScore  = 0;
	private static int coherentScore  = 0;
	private static int topicScore     = 0;
	private static int lengthScore    = 0;
	

	public static void main(String[] args) throws Exception
	{
		
//		PrintStream out;
//		try {
//			out = new PrintStream(new FileOutputStream("output/result.txt"));
//			System.setOut(out);
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		// Retrieve the names of the essays from the given directory
		File folder = new File(inputPath);
		File[] listOfFiles = folder.listFiles();
		String fileNames[];
		fileNames = new String[listOfFiles.length];

		for (int i = 0; i < listOfFiles.length; i++) 
		{
			if (listOfFiles[i].isFile()) 
			{
					fileNames[i] = listOfFiles[i].getName();
			} 
		}
		
		// Generate and display the score for each essay
		for(String name : fileNames)
		{
			try {
				generateScore(name);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		//languageToolTest();
		
	}
	public static int SentenceDetect(String paragraph) throws InvalidFormatException, IOException 
	{
		// Detect the number of sentences in an essay
		InputStream is = new FileInputStream("res/en-sent.bin");
		SentenceModel model = new SentenceModel(is);
		SentenceDetectorME sdetector = new SentenceDetectorME(model);

		String sentences[] = sdetector.sentDetect(paragraph);
		is.close();
		return sentences.length;
	}
	
	public static String[] Tokenize(String paragraph) throws InvalidFormatException, IOException 
	{	
		// Tokenize the sentence and return an array of tokens
		InputStream is = new FileInputStream("res/en-token.bin");
		TokenizerModel model = new TokenizerModel(is);
		Tokenizer tokenizer = new TokenizerME(model);
		String tokens[] = tokenizer.tokenize(paragraph);
	 
		is.close();
		return tokens;
	}
	

	public static String[] generateTags(String[] words)
	{
		
		// Generate an array of POS tags
		String[] tags = null;
		InputStream modelIn = null;

		try {
		  modelIn = new FileInputStream("res/en-pos-maxent.bin");
		  POSModel model = new POSModel(modelIn);
		  POSTaggerME tagger = new POSTaggerME(model);
		  tags = tagger.tag(words);
		}
		catch (IOException e) {
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
	
	// Supposed to count the number of verb errors per essay. Does not work at all.
	// Still working on it.  Supposed to check for verb agreement in number, but 
	// I'm having trouble identifying which nouns are subjects and which are not.
	// Gives a lot of false positives for verb agreement errors.
	public static int countErrors(String[] tags)
	{
		int count = 0;
		int j = 0;
		boolean inPrepPhrase = false;
		boolean found = false;
		
		for (int i = 0; i < tags.length-1; i++)
		{
			if(found)
			{
				while(i < tags.length-1 && !tags[i].equals("."))
					i++;
				found = false;
			}
			else if(tags[i].equals("IN"))
			{
				inPrepPhrase = true;
			}
			else if(tags[i].charAt(0) == 'N')
			{
				//System.out.println("Have " + tags[i]);
				if(inPrepPhrase)
				{
					inPrepPhrase = false;
				}
				else
				{
					found = false;
					j = i+1;
					if(tags[i].equals("NN") || tags[i].equals("NNP"))
					{
						while(!found && j<tags.length)
						{
							switch(tags[j])
							{
							case "VBP":
							case "VBZ": found = true;
										break;
							case "VB": subVerbErrors++;
										found = true;
										break;
							case ".": found = true;
										verbTenseErrors++;
										break;
							default: j++;
										break;
							}
						}
					}
					else if(tags[i].equals("NNS") || tags[i].equals("NNPS"))
					{
						while(!found && j<tags.length)
						{
								switch(tags[j])
								{
								case "VBP":
								case "VBZ": found = true;
										subVerbErrors++;
										break;
								case "VB": 	found = true;
										break;
								case ".": found = true;
										verbTenseErrors++;
										break;
								default: j++;
										break;
							}
						}
					}
				}
			}
		}
			
//			if((tags[i].equals("NN") && tags[i+1].equals("VBP"))
//					|| (tags[i].equals("NNS") && tags[i+1].equals("VBP")))
//				count++;
		
		
		return count;
	}

	public static void countErrors2(String[] tags)
	{
		boolean foundVerb;
		for(int i=0; i<tags.length-1; i++)
		{
			foundVerb = false;
			while(!tags[i].equals(".") && i<tags.length-1)
			{
				if(tags[i].equals("VBZ") || tags[i].equals("VBP") || tags[i].equals("VB"))
					foundVerb = true;
				
				if((tags[i].equals("MD") && tags[i+2].equals("VBZ")) || (tags[i].equals("MD") && tags[i+1].equals("VBZ")))
				{
					verbTenseErrors++;
				}
				else if(tags[i].equals("MD") && tags[i+1].equals("VBP"))
					verbTenseErrors++;
				else if(tags[i].equals("NN") || tags[i].equals("NNP"))
				{
					if(tags[i+1].equals("VB") || tags[i].equals("VBP"))
						subVerbErrors++;
				}
				else if(tags[i].equals("NNS") || tags[i].equals("NNPS"))
				{
					if(tags[i+1].equals("VBZ") || tags[i+1].equals("VBP"))
						subVerbErrors++;
					if(i<(tags.length-2) && (tags[i+2].equals("VBZ") || tags[i+2].equals("VBP")))
						subVerbErrors++;
				}
				else if(tags[i].equals("PRP") && tags[i+1].equals("VBG"))
					subVerbErrors++;
				else if(tags[i].equals("VBP") && (tags[i+1].equals("VBP") || tags[i+1].equals("VB")))
					verbTenseErrors++;
				else if(tags[i].equals("VB") && tags[i+1].equals("VBP"))
					verbTenseErrors++;
				
				
				i++;
			}
			if(!foundVerb)
				verbTenseErrors++;
		}
	}
	
	public static void generateScore(String filename) throws Exception
	{
		subVerbErrors = 0;
		verbTenseErrors = 0;
		String text = "";
		try(BufferedReader br = new BufferedReader(new FileReader(inputPath + filename))) 
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
		
		
		String tokens[] = Tokenize(text);
		String[] tags = generateTags(tokens);
		File dir = new File("res/spellchecker/");
		Directory directory = FSDirectory.open(dir);

		SpellChecker spellChecker = new SpellChecker(directory);

		spellChecker.indexDictionary(new PlainTextDictionary(new File("res/dictionary3.txt")));	
		
		int spellErrors = 0;
		for(int i = 0; i<tokens.length; i++)
		{
			//TODO Fix for two letter words, clitics, and punctuation
			if(tokens[i].length()>=3)
				if(!(spellChecker.exist(tokens[i].toLowerCase())))
				{
					spellErrors++;
				}
		}
		
		countErrors2(tags);
		int numSentences = SentenceDetect(text);
		
		double spellingErrorsPer = (((double)spellErrors)/(double)numSentences)*100;
		double verbAgreeErrorsPer =  (((double)subVerbErrors)/(double)numSentences)*100;
		double verbTenseErrorsPer = (((double)verbTenseErrors)/(double)numSentences)*100;
		
		int e1 = (int)spellingErrorsPer;
		int e2 = (int)verbAgreeErrorsPer;
		int e3 = (int)verbTenseErrorsPer;
		
		
		// Scale the spelling score to score 1-5
		if(e1<=40)
			spellingScore = 5;
		else if(e1<=80)
			spellingScore = 4;
		else if(e1<=120)
			spellingScore = 3;
		else if(e1<=200)
			spellingScore = 2;
		else
			spellingScore = 1;
		
		//Scale the length score to score 1-5
		if(numSentences > 17)
			lengthScore = 5;
		else if(numSentences > 15)
			lengthScore = 4;
		else if(numSentences > 12)
			lengthScore = 3;
		else if(numSentences > 9)
			lengthScore = 2;
		else 
			lengthScore = 1;
		
		if(e2 < 25)
			subVerbScore = 5;
		else if(e2 < 50)
			subVerbScore = 4;
		else if(e2 < 80)
			subVerbScore = 3;
		else if(e2 < 100)
			subVerbScore = 2;
		else
			subVerbScore = 1;
		
		if(e3 <10)
			verbTenseScore = 5;
		else if(e3<15)
			verbTenseScore = 4;
		else if(e3<20)
			verbTenseScore = 3;
		else if(e3<25)
			verbTenseScore = 2;
		else 
			verbTenseScore = 1;
		
		//Calculate total weighted score
		int totalScore = spellingScore + subVerbScore + verbTenseScore + 2*sentFormScore 
				+ 2*coherentScore + 3*topicScore + 2*lengthScore;
		
		String finalGrade = "Unknown";
		
//		if(totalScore > 32)
//			finalGrade = "High";
//		else if(totalScore >24)
//			finalGrade = "Medium";
//		else 
//			finalGrade = "Low";
		
		
		System.out.print(filename);
		System.out.print("\t" + spellingScore);
		System.out.print("\t" + subVerbScore);
		System.out.print("\t" + verbTenseScore);
		System.out.print("\t" + sentFormScore);
		System.out.print("\t" + coherentScore);
		System.out.print("\t" + topicScore);
		System.out.print("\t" + lengthScore);
		System.out.print("\t" + totalScore);
		System.out.println("\t" + finalGrade);
		
		
//		System.out.print(filename);
//		// Average sentence counts from training essays: High: 17 Med: 14.6 Low: 11.5
//		System.out.print("\tSentence Count: " + numSentences);
//		// Average spelling errors from training: High: 7.6  Med: 13.8   Low: 15.5
//		System.out.print("\tSpelling Errors: " + e1);
//		// Average errors: High:  Med:  Low:
//		System.out.print("\tVerb agreement errors: " + e2);
//		// Average errors: High:  Med:  Low:
//		System.out.println("\tVerb Tense, etc. errors: " + e3);
		spellChecker.close();

	}
	
//	public static void languageToolTest() throws Exception
//	{
//		JLanguageTool langTool = new JLanguageTool(new AmericanEnglish());
//		//langTool.activateDefaultPatternRules();  -- only needed for LT 2.8 or earlier
//		List<RuleMatch> matches = langTool.check("A sentence with a error in the Hitchhiker's Guide tot he Galaxy");
//		 
//		for (RuleMatch match : matches) {
//		  System.out.println("Potential error at line " +
//		      match.getLine() + ", column " +
//		      match.getColumn() + ": " + match.getMessage());
//		  System.out.println("Suggested correction: " +
//		      match.getSuggestedReplacements());
//		}
//
//		
//		
//		
//	}
}
