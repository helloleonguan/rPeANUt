
public class ParseError {
    Lineinfo line;
    String description;
	
	public ParseError(Lineinfo l, String d) {
    	 line = l;
    	 description = d;
     }
	@Override
	public String toString() {
		
		return line.toString() + ":" + description;
	}
}
 