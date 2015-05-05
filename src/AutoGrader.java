import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import opennlp.tools.parser.Parse;
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
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;



/**
 * Automatically grades essays based on spelling and grammar
 * @author Peter Hanula, Matt Healy
 */
public class AutoGrader
{
	// Path to directory containing the essays to be graded
	private static String inputPath = "input/test/original/";
	// The number of subject-verb agreement errors in the essay
	private static int subVerbErrors = 0;
	// The number of verb-tense, missing verb, and extra verb errors in the essay
	private static int verbTenseErrors = 0;
	// The number of sentence formation errors
	private static int sentenceFormErrors = 0;
	//
	private static int coherencePercentage = 0;
	
	
	// Fields to hold the various scores (1-5) for the essay
	private static int spellingScore  = 0;
	private static int subVerbScore   = 0;
	private static int verbTenseScore = 0;
	private static int sentFormScore  = 0;
	private static int coherentScore  = 0;
	private static int topicScore     = 0;
	private static int lengthScore    = 0;
	
	private static String sentences[];
	private static ArrayList<Parse> parses;
	

	public static void main(String[] args) throws Exception
	{
		
		// Change the System.out printstream so that output is sent to the file
		// 'reult.txt'
		PrintStream out;
		parses = new ArrayList<Parse>();
		
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
		
		Properties props = new Properties();
		props.setProperty("annotators",
				"tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		
		//props.put("dcoref.score", true);
		
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		
//		for (String name : fileNames) {
//			try {
//				//System.out.println("" + name);
//				StanfordChecker.generateScore(name, pipeline);
//				//System.out.println("");
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
		
		// Generate and display the score for each essay in the directory
		for(String name : fileNames)
		{
			try {
				generateScore(name, pipeline);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		System.err.println("Program finished");
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

		paragraph = paragraph.replace(".", ". ");
		
		sentences = sdetector.sentDetect(paragraph);
		is.close();
		
		NLPParser p = new NLPParser();
		
		sentenceFormErrors = 0;

		for(String s:sentences)
		{
			Parse temp = p.parseSentence(s);
			parses.add(temp);
			StringBuffer s2 = new StringBuffer();
			temp.show(s2);
			String s3 = s2.substring(0, 8);
			if(!(s3.equals("(TOP (S ")))
				sentenceFormErrors++;
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
	public static void generateScore(String filename, StanfordCoreNLP pipeline) throws Exception
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
		
		
		String text2 = text.replace(".", ". ");
		
		int countSingleRefer = 0;
		int countMultipleRefer = 0;
		int countSingleAdded = 0;
		int countMultipleAdded = 0;
		
		
		Map<Integer, CorefChain> graph = StanfordChecker.coreferenceResolution(text2, pipeline);
		for (Entry<Integer, CorefChain> entry : graph.entrySet()) {
			//System.out.println(entry.getValue());
			//System.out.print(entry.getValue().getMentionsInTextualOrder().size() + ", ");
			
			if (entry.getValue().getMentionsInTextualOrder().size() == 1) {
				countSingleRefer++;
				countSingleAdded += entry.getValue().getMentionsInTextualOrder().size();
			}
			else {
				countMultipleRefer++;
				countMultipleAdded += entry.getValue().getMentionsInTextualOrder().size();
			}
			
		}
		
		//System.out.println("");
		//System.out.println("countSingleRefer:   " + countSingleRefer);
		//System.out.println("countMultipleRefer: " + countMultipleRefer);
		//System.out.println("           ratio:   " + (double)countSingleRefer / (double)countMultipleRefer);
		//System.out.println("countSingleAdded:   " + countSingleAdded);
		//System.out.println("countMultipleAdded: " + countMultipleAdded);
		//System.out.println("           ratio:   " + (double)countSingleAdded / (double)countMultipleAdded);
		
		double ratio1 = (double)countSingleRefer / (double)countMultipleRefer;
		double ratio2 = (double)countSingleAdded / (double)countMultipleAdded;
		
		double totalAdd1 = ratio1 + ratio2;
//		System.err.println(totalAdd1);
//		double totalDiv1 = ratio1 / ratio2;
//		double totalDiv2 = ratio2 / ratio1;
		
//		System.out.println(" TotalAdd1: " + totalAdd1);
//		System.out.println(" TotalDiv1: " + totalDiv1);
//		System.out.println(" TotalDiv2: " + totalDiv2);
		
		
		// Generate the tokens for the given text
		String tokens[] = Tokenize(text);
		// Generate the POS tags for the given essay
		String[] tags = generateTags(tokens);
		
		coherencePercentage = TopicCoherenceChecker.checkCoherence(tokens, tags);
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
		
		// Find the number of verb errors in the essay
		// Results stored in static variables
		countErrors2(tags);
		
		subVerbErrors += LanguageToolTest.grammarChecker(text, "grammar", "agreement");
		subVerbErrors /= 2;
		
		// Count the number of sentences in the essay
		int numSentences = SentenceDetect(text);
		
		// Normalize the error counts to adjust for length of essay
		// Returned value for final error counts is in the form
		// Errors per 100 sentences 
		spellErrors = (spellErrors+spellErrorsLT)/2;
		double spellingErrorsPer = (((double)spellErrors)/(double)numSentences)*100;
		double verbAgreeErrorsPer =  (((double)subVerbErrors)/(double)numSentences)*100;
		double verbTenseErrorsPer = (((double)verbTenseErrors)/(double)numSentences)*100;
		double sentFormErrorsPer = (((double)sentenceFormErrors)/(double)numSentences)*100;
		int e1 = (int)spellingErrorsPer;
		int e2 = (int)verbAgreeErrorsPer;
		int e3 = (int)verbTenseErrorsPer;
		int e4 = (int)sentFormErrorsPer;
		
		//Spelling Error Training Data
		// Low: 154 23 93 86 76  23 580 41 285  43 => 140.4
		// Med: 192 44 43 40 27 228  55 52  50 138 =>  86.9
		// Hi :  15 30 20 42  4  57  71 40  15 115 =>  40.9
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
		
		//Sentence Length Training Data
		// Low: 11 13 15 15 26 17  5 12  7 16 => 13.7
		// Med: 14 29 16 15 11  7 18 17 14 13 => 15.4
		// Hi : 19 20 15 14 25 14 14 20 20 13 => 17.4
		if(numSentences >= 19)
			lengthScore = 5;
		else if(numSentences > 15)
			lengthScore = 4;
		else if(numSentences > 12)
			lengthScore = 3;
		else if(numSentences > 10)
			lengthScore = 2;
		else 
			lengthScore = 1;
		
		//Subject Verb Agreement Training Data
		// Low: 27 15 33 13 11 11 40 8  14 12 => 18.4
		// Med: 21 17  0 20 36 42 16 23 14 23 => 21.2
		// Hi : 15 25 26 21  0 28 28 25  5 15 => 18.8
		if(e2 < 20)
			subVerbScore = 5;
		else if(e2 < 25)
			subVerbScore = 4;
		else if(e2 < 30)
			subVerbScore = 3;
		else if(e2 < 35)
			subVerbScore = 2;
		else
			subVerbScore = 1;
		
		//Verb Tense etc. Training Data
		// Low:  9 7 0 33  0  0 20  0 0 6 => 7.5
		// Med: 14 3 6 20  0 14  0 11 0 0 => 6.8
		// Hi : 21 0 0  0 16  7  0  0 5 7 => 5.6
		if(e3 <=5)
			verbTenseScore = 5;
		else if(e3<=12)
			verbTenseScore = 4;
		else if(e3<=20)
			verbTenseScore = 3;
		else if(e3<=25)
			verbTenseScore = 2;
		else 
			verbTenseScore = 1;
		
		//Normalized Sentence Errors Per
		// Low: 33 7 0 33 13 6 0 0 28 6  => 12.6
		// Med: 14 3 26 6 0 14 5 11 0 0  =>  7.9
		// Hi : 15 10 0 14 4 0 7 0 15 16 =>  8.1
		if(e4<8)
			sentFormScore = 5;
		else if(e4<10)
			sentFormScore = 4;
		else if(e4<15)
			sentFormScore = 3;
		else if(e4<20)
			sentFormScore = 2;
		else
			sentFormScore = 1;

		//Coherence Percentages
		// Low: 16 12 16 11 22 13 14  5 11  8 => 12.8
		// Med: 13 17 13 16 15  5 12 16 19 12 => 13.8
		// Hi : 14 14  9 13 22 22 12 23  8  7 => 14.6
		if(coherencePercentage > 18)
			topicScore = 5;
		else if(coherencePercentage >=14)
			topicScore = 4;
		else if(coherencePercentage >=10)
			topicScore = 3;
		else if(coherencePercentage >=8)
			topicScore = 2;
		else
			topicScore = 1;
		
		if(totalAdd1 <3.5)
			coherentScore = 5;
		else if(totalAdd1 < 5)
			coherentScore = 4;
		else if(totalAdd1 < 6.5)
			coherentScore = 3;
		else if(totalAdd1 < 7)
			coherentScore = 2;
		else coherentScore = 1;
		
		//Calculate total weighted score
		int totalScore = spellingScore + subVerbScore + verbTenseScore + 2*sentFormScore 
				+ 2*coherentScore + 3*topicScore + 2*lengthScore;
		
		String finalGrade = "Unknown";
		if(totalScore < 37)
			finalGrade = "Low";
		else if(totalScore < 47)
			finalGrade = "Medium";
		else
			finalGrade = "High";
		
		
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
		
		
		//System.err.println(e2);
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
