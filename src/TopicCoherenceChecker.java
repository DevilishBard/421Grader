import java.util.ArrayList;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;


public class TopicCoherenceChecker 
{
	public static int checkCoherence(String[] tokens, String[] tags) 
	{
		 
		System.setProperty("wordnet.database.dir", "res/coherence/dict/");
		WordNetDatabase database = WordNetDatabase.getFileInstance();
 
		NounSynset nounSynset; 
		Synset[] hyponyms = null; 
		ArrayList<String> related = new ArrayList<String>();

		Synset[] synsets = database.getSynsets("cars", SynsetType.NOUN); 
		for (int i = 0; i < synsets.length; i++) { 
		    nounSynset = (NounSynset)(synsets[i]); 
		    hyponyms = nounSynset.getMemberMeronyms();
		    for(Synset s: hyponyms)
			{
				for(String word : s.getWordForms())
					related.add(word);
			}
		}
		for (int i = 0; i < synsets.length; i++) { 
		    nounSynset = (NounSynset)(synsets[i]); 
		    hyponyms = nounSynset.getHypernyms();
		    for(Synset s: hyponyms)
			{
				for(String word : s.getWordForms())
					related.add(word);
			}
		}
		for (int i = 0; i < synsets.length; i++) { 
		    nounSynset = (NounSynset)(synsets[i]); 
		    hyponyms = nounSynset.getMemberHolonyms();
		    for(Synset s: hyponyms)
			{
				for(String word : s.getWordForms())
					related.add(word);
			}
		}
		for (int i = 0; i < synsets.length; i++) { 
		    nounSynset = (NounSynset)(synsets[i]); 
		    NounSynset[] ant = nounSynset.getPartMeronyms();
		    for(NounSynset s: ant)
			{
		    	for(String word : s.getWordForms())
					related.add(word);
			}
		}
		for (int i = 0; i < synsets.length; i++) { 
		    nounSynset = (NounSynset)(synsets[i]); 
		    NounSynset[] ant = nounSynset.getSubstanceMeronyms();
		    for(NounSynset s: ant)
			{
		    	for(String word : s.getWordForms())
					related.add(word);
			}
		}
//		for(String s: related)
//			System.out.println(s);
		
		//ArrayList<String> nouns = new ArrayList<String>();
		int count = 0;
		int nouns = 0;
		related.add("cars");
		related.add("Cars");
		related.add("car");
		related.add("Car");
		for(String s: tokens)
			if(related.indexOf(s) >= 0)
				count ++;
		
		for(String s: tags)
			if(s.charAt(0) == 'N')
				nouns++;
		
		int percentRelated = (int)(((double)count)/((double)nouns)*100);
		return percentRelated;
		
 
	}
}
