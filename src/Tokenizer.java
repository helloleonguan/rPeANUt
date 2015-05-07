import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;

/**
 * Tokenizer - this uses the StreamTokenizer class to make a simpler tokenizer
 * which provides a stream of tokens which are either Integer, Double, or
 * String.
 * 
 * @author Eric McCreath
 * 
 */

public abstract class Tokenizer {

	abstract boolean hasCurrent();

	abstract Object current();

	abstract void next();

	abstract public String nextLine();

	abstract public String nextLinePeek();
	
	abstract public int currentStart();
	abstract public int currentEnd();
	
	

}
