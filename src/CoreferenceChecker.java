import java.awt.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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
	
	public CoreferenceChecker()
	{

		try {
			_linker = new DefaultLinker("res/coref", LinkerMode.TEST);
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public DiscourseEntity[] findEntityMentions(final String[] sentences,
		      final String[][] tokens, Parse p) {
		   // tokens should correspond to sentences
		   assert(sentences.length == tokens.length);
		    
		   // list of document mentions
		   final ArrayList<Mention> document = new ArrayList<Mention>();
		 
		   for (int i=0; i < sentences.length; i++) {
		      // generate the sentence parse tree
		      final Parse parse = p;
		       
		      final DefaultParse parseWrapper = new DefaultParse(parse, i);
		      final Mention[] extents = _linker.getMentionFinder().getMentions(parseWrapper);
		 
		      for (int ei=0, en=extents.length; ei<en; ei++) {
		         if (extents[ei].getParse() == null) {
		            final Parse snp = new Parse(parse.getText(), 
		                  extents[ei].getSpan(), "NML", 1.0, 0);
		            parse.insert(snp);
		            extents[ei].setParse(new DefaultParse(snp, i));
		         }
		      }
		      document.addAll(Arrays.asList(extents));
		   }
		 
		   if (!document.isEmpty()) {
		      return _linker.getEntities(document.toArray(new Mention[0]));
		   }
		   return new DiscourseEntity[0];
		}
}
