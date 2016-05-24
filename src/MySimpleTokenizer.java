import java.util.ArrayList;

/**
 * MySimpleTokenizer - this tokenizer uses some simple check to break up the
 * tokens the text. The Integer and Double parsers are used to parse numbers.
 * 
 * 
 * @author Eric McCreath - GPLv2
 */

public class MySimpleTokenizer extends Tokenizer {
	private String text;
	private int pos;
	private Object current;
	private int currentstart;
	private int currentend;
	private Defines defines;
	
	private ArrayList<Object> pushback;

	static final char whitespace[] = { ' ', '\t' };
	static final char symbol[] = { ':', '#', '&', '\n' };
	static final char hexdig[] = { 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C',
			'D', 'E', 'F' };

	public MySimpleTokenizer(Defines defines,  String text) {
		this.text = text;
		this.pos = 0;
		this.defines = defines;
		pushback = new ArrayList<Object>();
		next();
	}

	boolean hasCurrent() {
		return current != null;
	}

	Object current() {
		return current;
	}

	public void next() {
		if (pushback.size() > 0) {
			current = pushback.remove(0);
			return;
		}
		
		consumewhite();
		currentstart = pos;
		if (pos >= text.length()) {
			current = null;
		} else if (text.charAt(pos) == ';') { // everything after this is a
												// comment
			while (pos < text.length() && text.charAt(pos) != '\n')
				pos++;
			if (pos == text.length()) {
				current = null;
			} else {
				pos++;
				current = "\n";
			}
		} else if (isin(text.charAt(pos), symbol)) {
			current = "" + text.charAt(pos);
			pos++;
		} else if (text.charAt(pos) == '\"') {
			String res = "\"";
			int start = pos;
			pos++;
			boolean escape = false;
			while (pos < text.length() && (escape || text.charAt(pos) != '\"')) {
				if (escape) {
					res += convertescape(text.charAt(pos));
					escape = false;
				} else {
					if (text.charAt(pos) == '\\') {
						escape = true;
					}  else {
					    res += text.charAt(pos);
					}
				}
				pos++;
			}
			if (pos < text.length() &&  text.charAt(pos) == '\"') res += "\"";
			pos++;
			current = res;
		} else if (text.charAt(pos) == '\'') {
			
			
			
			if ((pos+3) < text.length() && (text.charAt(pos+1) == '\\') &&  (text.charAt(pos+3) == '\'') ) {
				
				current = "\'"  + convertescape(text.charAt(pos+2))  + "\'";
				pos +=4;
			} else if (((pos+2) < text.length()) && (text.charAt(pos+2) == '\'')) {
				current = "\'"  + text.charAt(pos+1)  + "\'";
				pos +=3;
			} else {
				current = "\'problem";
				pos++;
			}
		} else if (Character.isDigit(text.charAt(pos))) {
			int start = pos;
			if (pos + 1 < text.length() && text.charAt(pos + 1) == 'x') {
				pos += 2;
				start = pos;
				while (pos < text.length()
						&& (Character.isDigit(text.charAt(pos)) || isin(
								text.charAt(pos), hexdig)))
					pos++;
				Long w;
				try {
					w = Long.parseLong(text.substring(start, pos), 16);
					current = (Integer) (int) (w & 0xffffffff);
				} catch (NumberFormatException nf2) {
					w = null;
					current = null;
				}
				
			} else {
				while (pos < text.length()
						&& Character.isDigit(text.charAt(pos)))
					pos++;
				try {
				current = Integer.parseInt(text.substring(start, pos));
				} catch (NumberFormatException nfe) {
					current = null;
				}
			}
		} else if (text.charAt(pos) == '-') {
			int start = pos;
			pos++;
			while (pos < text.length() && Character.isDigit(text.charAt(pos)))
				pos++;
			try {
			   current = Integer.parseInt(text.substring(start, pos));
			} catch (NumberFormatException nfe) {
			   current = null;
			}
		} else {
			int start = pos;
			while (pos < text.length() && !isin(text.charAt(pos), symbol)
					&& !isin(text.charAt(pos), whitespace))
				pos++;
			String str = text.substring(start, pos);
			
			if (defines.containsKey(str)) {
				ArrayList<Object> deflist =  defines.get(str);
				if (deflist.size() == 0) {
					next();
				} else if (deflist.size() == 0) {
					current = deflist.get(0);
				} else {
					current = deflist.get(0);
					for (int i =1 ;i< deflist.size();i++) pushback.add(deflist.get(i));
				}
			} else {
				current = str;
			}
			
		}
		currentend = pos;
	}

	static private String convertescape(char c) {
		if (c == 'n') {
			return "\n";
		} else if (c == '0') {
			return "\0";
		} else if (c == 't') {
			return "\t";
		} else {
			return "" + c;
		}

	}

	private void consumewhite() {
		while (pos < text.length() && isin(text.charAt(pos), whitespace))
			pos++;
	}

	private boolean isin(char c, char charlist[]) {
		for (char w : charlist) {
			if (w == c)
				return true;
		}
		return false;
	}

	// This consumes and return all the characters from the current position to
	// the
	// end of the line moving the tokenizer onto the next line
	@Override
	public String nextLine() {
		int spos = pos;
		while (pos < text.length() && !(text.charAt(pos) == '\n'))
			pos++;
		int epos = pos;
		next();
		return text.substring(spos, epos);
	}

	@Override
	public String nextLinePeek() {
		int spos = pos;
		while (pos < text.length() && !(text.charAt(pos) == '\n'))
			pos++;
		int epos = pos;
		pos = spos;
		//System.out.println("text:" + text + ":" + epos + ":" + spos);
		if (spos == epos) return "";
		else 
		return text.substring(spos, epos);
	}

	@Override
	public int currentStart() {

		return currentstart;
	}

	@Override
	public int currentEnd() {

		return currentend;
	}
}
