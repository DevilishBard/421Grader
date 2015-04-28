

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
public class LanguageToolTest
{
	// Path to directory containing the essays to be graded
	private static String inputPath = "input/training/low/";
	

	public static void main(String[] args) throws Exception
	{
		
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
		
		
		System.out.println("filename|spellErrorsLT|agreementChecker|typosChecker|grammarChecker|collocationsChecker|miscellaneousChecker");
		System.out.println("     punctuationChecker|commonConfusedChecker|nonPhrasesChecker|slangChecker|rePhrasesChecker|badStyleChecker|semanticChecker|plainEnglishChecker|totalChecker");
		
		// Generate and display the score for each essay in the directory
		for(String name : fileNames)
		{
			try {
				generateScore(name);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	/**
	 * Analyzes and scores an essay with the given filename
	 * @param filename The name of the file to be analyzed
	 * @throws Exception
	 */
	public static void generateScore(String filename) throws Exception
	{
		
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
		
		
		
		
		// language tool (LT), getting spelling and grammar error counts
		
		int spellErrorsLT = spellCheckerLanguageTool(text);
		
		int agreementChecker = grammarChecker(text, "grammar", "agreement");

		int typosChecker           = grammarChecker(text, "Possible Typo", "");
		int grammarChecker         = grammarChecker(text, "Grammar", "");
		int collocationsChecker    = grammarChecker(text, "Collocations", "");
		int miscellaneousChecker   = grammarChecker(text, "Miscellaneous", "");
		int punctuationChecker     = grammarChecker(text, "Punctuation Errors", "");
		int commonConfusedChecker  = grammarChecker(text, "Commonly Confused Words", "");
		int nonPhrasesChecker      = grammarChecker(text, "Nonstandard Phrases", "");
		int slangChecker           = grammarChecker(text, "Slang", "");
		int rePhrasesChecker       = grammarChecker(text, "Redundant Phrases", "");
		int badStyleChecker        = grammarChecker(text, "Bad style", "");
		int semanticChecker        = grammarChecker(text, "Semantic", "");
		int plainEnglishChecker    = grammarChecker(text, "Plain English", "");
		
		//int totalChecker           = grammarChecker(text);

		//int grammarChecker = grammarChecker(text);
		

		
		// Output the results of the analysis to standard out (result.txt)
		System.out.print(filename);
	    System.out.print("\t" + spellErrorsLT);
	    System.out.print("\t" + agreementChecker);
	    
		System.out.print("\t" + typosChecker);
		System.out.print("\t" + grammarChecker);
		System.out.print("\t" + collocationsChecker);
		System.out.print("\t" + miscellaneousChecker);
		System.out.print("\t" + punctuationChecker);
		System.out.print("\t" + commonConfusedChecker);
		System.out.print("\t" + nonPhrasesChecker);
		
		System.out.print("\t" + slangChecker);
		System.out.print("\t" + rePhrasesChecker);
		System.out.print("\t" + badStyleChecker);
		System.out.print("\t" + semanticChecker);
		System.out.print("\t" + plainEnglishChecker);
		//System.out.print("\t" + totalChecker);
		
		
		
		System.out.println("");
		

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
	
	public static int grammarChecker(String textInput, String category, String description) throws Exception
	{
		int grammarErrors = 0;
		
		JLanguageTool grammarCheck = new JLanguageTool(new AmericanEnglish());
		
		for (Rule rule : grammarCheck.getAllRules()) {

			
		  if (!(rule.getCategory().toString().toLowerCase().equals(category.toLowerCase()) && rule.getDescription().toString().toLowerCase().contains(description.toLowerCase()))) {
			  
			  grammarCheck.disableRule(rule.getId());
			  
			  // Prints out the specific rules/description for current check
			  //System.out.println("Category: " + rule.getCategory() + "  ID: " + rule.getId() + "\n      Name: " + rule.getDescription());
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
	
	public static int grammarChecker(String textInput) throws Exception
	{
		int grammarErrors = 0;
		
		JLanguageTool grammarCheck = new JLanguageTool(new AmericanEnglish());
		
		for (Rule rule : grammarCheck.getAllRules()) {
		  if (rule.isDictionaryBasedSpellingRule()) { // Enable just spell checking
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