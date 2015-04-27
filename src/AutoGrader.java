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

import opennlp.tools.parser.AbstractBottomUpParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.parser.chunking.Parser;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;



/**
 * Automatically grades essays based on spelling and grammar
 * @author Peter Hanula, Matt Healy
 */
public class AutoGrader
{
	// Path to directory containing the essays to be graded
	private static String inputPath = "input/training/low/";
	// The number of subject-verb agreement errors in the essay
	private static int subVerbErrors = 0;
	// The number of verb-tense, missing verb, and extra verb errors in the essay
	private static int verbTenseErrors = 0;
	// The number of sentence formation errors
	private static int sentenceFormErrors = 0;
	
	private Tokenizer _tokenizer;
	
	
	// Fields to hold the various scores (1-5) for the essay
	private static int spellingScore  = 0;
	private static int subVerbScore   = 0;
	private static int verbTenseScore = 0;
	private static int sentFormScore  = 0;
	private static int coherentScore  = 0;
	private static int topicScore     = 0;
	private static int lengthScore    = 0;
	

	public static void main(String[] args) throws Exception
	{
		
		// Change the System.out printstream so that output is sent to the file
		// 'reult.txt'
		PrintStream out;
		try {
			out = new PrintStream(new FileOutputStream("output/result.txt"));
			System.setOut(out);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
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
		
		
		// Generate and display the score for each essay in the directory
		for(String name : fileNames)
		{
			try {
				generateScore(name);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		//System.out.print("\n\n");
		
	}
	
	/**
	 * Counts the number of sentences in a given block of text
	 * @param paragraph The block of text to be analyzed for the number of sentences
	 * @return the number of sentences in the provided text block
	 * @throws InvalidFormatException
	 * @throws IOException
	 */
	public static int SentenceDetect(String paragraph) throws InvalidFormatException, IOException 
	{
		// Detect the number of sentences in an essay
		InputStream is = new FileInputStream("res/en-sent.bin");
		SentenceModel model = new SentenceModel(is);
		SentenceDetectorME sdetector = new SentenceDetectorME(model);

		String sentences[] = sdetector.sentDetect(paragraph);
		is.close();
		
		NLPParser p = new NLPParser();
		
		for(String s:sentences)
		{
			p.parseSentence(s);
		}
		
		return sentences.length;
	}
	
	
	/**
	 * Tokenizes the input block of text and returns an array of tokens
	 * @param paragraph The block of text to be tokenized
	 * @return An array of strings each string containing one token
	 * @throws InvalidFormatException
	 * @throws IOException
	 */
	public static String[] Tokenize(String paragraph) throws InvalidFormatException, IOException 
	{	
		InputStream is = new FileInputStream("res/en-token.bin");
		TokenizerModel model = new TokenizerModel(is);
		Tokenizer tokenizer = new TokenizerME(model);
		String tokens[] = tokenizer.tokenize(paragraph);
	 
		is.close();
		return tokens;
	}
	

	/**
	 * Generates POS tags for an array of tokens
	 * @param words An array of tokens to be tagged
	 * @return An array of POS tags corresponding the input tokens
	 */
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
	
	
	
		
	
	
	/**
	 * Analyzes an array of POS tags in order to find verb errors in a block of text
	 * @param tags An array of POS tags
	 */
	public static void countErrors2(String[] tags)
	{
		// Used to ensure that each sentence has a main verb
		boolean foundVerb;
		// Scan for sequences of tags that violate grammatical rules
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
	
	
	/**
	 * Analyzes and scores an essay with the given filename
	 * @param filename The name of the file to be analyzed
	 * @throws Exception
	 */
	public static void generateScore(String filename) throws Exception
	{
		subVerbErrors = 0;
		verbTenseErrors = 0;
		String text = "";
		
		// Read the file into a string 'text'
		try(BufferedReader br = new BufferedReader(new FileReader(inputPath + filename))) 
		//try(BufferedReader br = new BufferedReader(new FileReader(inputPath + "231566.txt"))) 
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
		
		// Generate the tokens for the given text
		String tokens[] = Tokenize(text);
		// Generate the POS tags for the given essay
		String[] tags = generateTags(tokens);
		// Create the directory for the spellchecker to use
		File dir = new File("res/spellchecker/");
		Directory directory = FSDirectory.open(dir);

		// Initialize the lucene spellchecker
		SpellChecker spellChecker = new SpellChecker(directory);

		// Index the provided dictionary for the spellchecker to use
		spellChecker.indexDictionary(new PlainTextDictionary(new File("res/dictionary3.txt")));	
		
		int spellErrors = 0;
		for(int i = 0; i<tokens.length; i++)
		{
			// Lucene spellchecker does not support spellchecking for words
			// Shorter than 3 characters
			if(tokens[i].length()>=3)
				if(!(spellChecker.exist(tokens[i].toLowerCase())) && !(spellChecker.exist(tokens[i])))
				{
					// U.S.A. should be added to the dictionary
					if(!tokens[i].equals("U.S.A."))
						spellErrors++;
				}
		}
		
		// language tool (LT), getting spelling and grammar error counts
		int spellErrorsLT = spellCheckerLanguageTool(text);
		//int grammarErrorsLT = grammarCheckerLanguageTool(text);
		//
		
		// Find the number of verb errors in the essay
		// Results stored in static variables
		countErrors2(tags);
		
		// Count the number of sentences in the essay
		int numSentences = SentenceDetect(text);
		
		// Normalize the error counts to adjust for length of essay
		// Returned value for final error counts is in the form
		// Errors per 100 sentences 
		
		spellErrors = (spellErrors+spellErrorsLT)/2;
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
		
		// Scale the number of subject-verb agreement errors 
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
		
		// Scale the number of verb tense, etc. errors
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
		
		// Temporarily provide unknown rating until part 2 is completed and 
		// essay quality can be properly judged and scored
		String finalGrade = "Unknown";
		
		// Output the results of the analysis to standard out (result.txt)
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
		
		
		//System.out.println("\t Spelling Errors default:  " + spellErrors);
		//System.out.println("\t Spelling Errors using LT: " + spellErrorsLT);
		//System.out.println("\t Grammar Errors using LT:  " + grammarErrorsLT);
		
		// Used to analyze numbers of errors for grade scaling
		// Not necessary for final program
//		System.out.print(filename);
//		// Average sentence counts from training essays: High: 17 Med: 14.6 Low: 11.5
//		System.out.print("\tSentence Count: " + numSentences);
//		// Average spelling errors from training: High: 7.6  Med: 13.8   Low: 15.5
//		System.out.print("\tSpelling Errors: " + spellErrors);
//		// Average errors: High:  Med:  Low:
//		System.out.print("\tVerb agreement errors: " + e2);
//		// Average errors: High:  Med:  Low:
//		System.out.println("\tVerb Tense, etc. errors: " + e3);
		
		spellChecker.close();

	}
	
	public static int spellCheckerLanguageTool(String textInput) throws Exception
	{
		int spellingErrors = 0;
		
		JLanguageTool spellCheck = new JLanguageTool(new AmericanEnglish());
		
		for (Rule rule : spellCheck.getAllRules()) {
		  if (!rule.isDictionaryBasedSpellingRule()) { // Enable just spell checking
			  spellCheck.disableRule(rule.getId());
		  }
		}
		
		List<RuleMatch> matches = spellCheck.check(textInput);
		for (@SuppressWarnings("unused") RuleMatch match : matches) {
//		  System.out.println("Potential typo at line " +
//		          match.getLine() + ", column " +
//		          match.getColumn() + ": " + match.getMessage());
//		  System.out.println("Suggested correction(s): " +
//		          match.getSuggestedReplacements());
			spellingErrors++;
		}
		
		return spellingErrors;
	}
	
	public static int grammarCheckerLanguageTool(String textInput) throws Exception
	{
		int grammarErrors = 0;
		
		JLanguageTool grammarCheck = new JLanguageTool(new AmericanEnglish());
		
		for (Rule rule : grammarCheck.getAllRules()) {
		  if (rule.isDictionaryBasedSpellingRule()) { // Disable spell checking
			  grammarCheck.disableRule(rule.getId());
		  }
		}
		
		List<RuleMatch> matches = grammarCheck.check(textInput);
		for (@SuppressWarnings("unused") RuleMatch match : matches) {
//		  System.out.println("Potential typo at line " +
//		          match.getLine() + ", column " +
//		          match.getColumn() + ": " + match.getMessage());
//		  System.out.println("Suggested correction(s): " +
//		          match.getSuggestedReplacements());
		  grammarErrors++;
		}
		
		return grammarErrors;
	}
}
