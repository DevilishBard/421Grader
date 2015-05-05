import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import opennlp.tools.coref.DefaultLinker;
import opennlp.tools.coref.DiscourseEntity;
import opennlp.tools.coref.Linker;
import opennlp.tools.coref.LinkerMode;
import opennlp.tools.coref.mention.DefaultParse;
import opennlp.tools.coref.mention.Mention;
import opennlp.tools.parser.Parse;


public class CoreferenceChecker 
{
	Linker _linker = null;
	NLPParser p = null;
	
	public CoreferenceChecker()
	{

		try {
			_linker = new DefaultLinker("res/coref", LinkerMode.TEST);
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}
		
		p = new NLPParser();
	}
	
	 public DiscourseEntity[] findEntityMentions(final String[] sentences) {
	      
	      // list of document mentions
	      final ArrayList<Mention> document = new ArrayList<Mention>();

	      for (int i=0; i < sentences.length; i++) {
	         // generate the sentence parse tree
	         final Parse parse = p.parseSentence(sentences[i]);
	         System.out.println("From fEM");
	         parse.show();
	         final DefaultParse parseWrapper = new DefaultParse(parse, i);
	         final Mention[] extents = linker().getMentionFinder().getMentions(parseWrapper);
	         
	         //Note: taken from TreebankParser source...
	         for (int ei=0, en=extents.length; ei<en; ei++) {
	            // construct new parses for mentions which don't have constituents.
	            if (extents[ei].getParse() == null) {
	               // not sure how to get head index, but its not used at this point
	               final Parse snp = new Parse(parse.getText(), extents[ei].getSpan(), "NML", 1.0, 0);
	               parse.insert(snp);
	               extents[ei].setParse(new DefaultParse(snp, i));
	            }
	         }
	         document.addAll(Arrays.asList(extents));
	      }
	      
	      if (!document.isEmpty()) {
	    	  try
	    	  {
	    		  return linker().getEntities(document.toArray(new Mention[1]));
	    	  }
	    	  catch(NoSuchElementException e)
	    	  {
	    		  System.err.println("No such element");
	    	  }
	      }
	      return new DiscourseEntity[0];
	   }
	   
	   /**
	    * @return the lazily-initialized linker
	    */
	   private Linker linker() {
	      if (_linker == null) {
	         try {
	            // linker
	            
	            _linker = new DefaultLinker("res/coref", LinkerMode.TEST);
	            
	         } catch (final IOException ioe) {
	           System.err.println("GAHHHH");
	         }
	      }
	      return _linker;
	   }
	
//	public DiscourseEntity[] findEntityMentions(ArrayList<Parse> p) {
//		   
//		    
//		   final ArrayList<Mention> document = new ArrayList<Mention>();
//		 
//		   for (int i=0; i < p.size(); i++) 
//		   {
//		      final Parse parse = p.get(i);
//		       
//		      final DefaultParse parseWrapper = new DefaultParse(parse, i);
//		      final Mention[] extents = _linker.getMentionFinder().getMentions(parseWrapper);
//		 
//		      for (int ei=0, en=extents.length; ei<en; ei++) {
//		         if (extents[ei].getParse() == null) {
//		            final Parse snp = new Parse(parse.getText(), 
//		                  extents[ei].getSpan(), "NML", 1.0, 0);
//		            parse.insert(snp);
//		            extents[ei].setParse(new DefaultParse(snp, i));
//		         }
//		      }
//		      document.addAll(Arrays.asList(extents));
//		      System.err.println("Did this");
//		   }
//		 
//		   if (!document.isEmpty()) 
//		   {
//			   System.err.println("Doc isn't empty");
//			   return _linker.getEntities(document.toArray(new Mention[0]));
//		   }
//		   return new DiscourseEntity[0];
//		}
}
