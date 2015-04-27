import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.parser.AbstractBottomUpParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.parser.chunking.Parser;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;


public class NLPParser 
{
	Tokenizer _tokenizer;
	
	public NLPParser()
	{
		InputStream modelIn = null;
		try {
		   // Loading tokenizer model
		   modelIn = new FileInputStream("res/en-token.bin");
		   final TokenizerModel tokenModel = new TokenizerModel(modelIn);
		   modelIn.close();
		 
		   _tokenizer = new TokenizerME(tokenModel);
		 
		} catch (final IOException ioe) {
		   ioe.printStackTrace();
		} finally {
		   if (modelIn != null) {
		      try {
		         modelIn.close();
		      } catch (final IOException e) {} // oh well!
		   }
		}
	}
	
	
	public Parse parseSentence(final String text) {
		
		   int count = 0;
		   final Parse p = new Parse(text,
			         // a new span covering the entire text
			         new Span(0, text.length()),
			         // the label for the top if an incomplete node
			         AbstractBottomUpParser.INC_NODE,
			         // the probability of this parse...uhhh...? 
			         1,
			         // the token index of the head of this parse
			         0);
			 
			   // make sure to initialize the _tokenizer correctly
			   final Span[] spans = _tokenizer.tokenizePos(text);
			 
			   for (int idx=0; idx < spans.length; idx++) {
			      final Span span = spans[idx];
			      // flesh out the parse with individual token sub-parses 
			      p.insert(new Parse(text,
			            span,
			            AbstractBottomUpParser.TOK_NODE, 
			            0,
			            idx));
			   }
			   StringBuffer s = new StringBuffer();
			   Parse actualParse = parse(p);
			   return actualParse;
			}
	
	private Parser _parser = null;
	 
	private Parse parse(final Parse p) {
	   // lazy initializer
	   if (_parser == null) {
	      InputStream modelIn = null;
	      try {
	         // Loading the parser model
	         modelIn = new FileInputStream("res/en-parser-chunking.bin");
	         final ParserModel parseModel = new ParserModel(modelIn);
	         modelIn.close();
	          
	         _parser = (Parser) ParserFactory.create(parseModel);
	      } catch (final IOException ioe) {
	         ioe.printStackTrace();
	      } finally {
	         if (modelIn != null) {
	            try {
	               modelIn.close();
	            } catch (final IOException e) {} // oh well!
	         }
	      }
	   }
	   if(_parser != null)
		   return _parser.parse(p);
	   else return null;
	}
}
