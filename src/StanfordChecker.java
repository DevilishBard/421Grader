import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import opennlp.tools.parser.Parse;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
public class StanfordChecker {

	// Path to directory containing the essays to be graded
	private static String inputPath = "input/training/high/";
	
	//private static String sentences[];
	
	//private static ArrayList<Parse> parses;
	

	public static void generateScore(String filename, StanfordCoreNLP pipeline) throws Exception {

		String text = "";

		// Read the file into a string 'text'
		try (BufferedReader br = new BufferedReader(new FileReader(inputPath
				+ filename)))
		// try(BufferedReader br = new BufferedReader(new FileReader(inputPath +
		// "231566.txt")))
		{
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			text = sb.toString();
		}
		
		//System.out.println(text);

		//SentenceDetect(text);
		
		text = text.replace(".", ". ");
		
		int countSingleRefer = 0;
		int countMultipleRefer = 0;
		int countSingleAdded = 0;
		int countMultipleAdded = 0;
		
		
		Map<Integer, CorefChain> graph = coreferenceResolution(text, pipeline);
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
		
		System.out.println("");
		//System.out.println("countSingleRefer:   " + countSingleRefer);
		//System.out.println("countMultipleRefer: " + countMultipleRefer);
		//System.out.println("           ratio:   " + (double)countSingleRefer / (double)countMultipleRefer);
		//System.out.println("countSingleAdded:   " + countSingleAdded);
		//System.out.println("countMultipleAdded: " + countMultipleAdded);
		//System.out.println("           ratio:   " + (double)countSingleAdded / (double)countMultipleAdded);
		
		double ratio1 = (double)countSingleRefer / (double)countMultipleRefer;
		double ratio2 = (double)countSingleAdded / (double)countMultipleAdded;
		
		double totalAdd1 = ratio1 + ratio2;
		double totalDiv1 = ratio1 / ratio2;
		double totalDiv2 = ratio2 / ratio1;
		
		System.out.println(" TotalAdd1: " + totalAdd1);
		System.out.println(" TotalDiv1: " + totalDiv1);
		System.out.println(" TotalDiv2: " + totalDiv2);
		
	}

	public static Map<Integer, CorefChain> coreferenceResolution(String text, StanfordCoreNLP pipeline) {
		
		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);

		return document.get(CorefChainAnnotation.class);
	}

	public static void main(String[] args) {

		// Retrieve the names of the essays from the given directory
		File folder = new File(inputPath);
		File[] listOfFiles = folder.listFiles();
		String fileNames[];
		fileNames = new String[listOfFiles.length];

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				fileNames[i] = listOfFiles[i].getName();
			}
		}
		
		Properties props = new Properties();
		props.setProperty("annotators",
				"tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		
		//props.put("dcoref.score", true);
		
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		
		for (String name : fileNames) {
			try {
				System.out.println("" + name);
				generateScore(name, pipeline);
				System.out.println("");
				//generateScore(fileNames[3], pipeline);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	

}