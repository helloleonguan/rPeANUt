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

	static final char whitespace[] = { ' ', '\t' };
	static final char symbol[] = { ':', '#', '&', '\n' };
	static final char hexdig[] = { 'a',  'b', 'c', 'd', 'e', 'f','A',  'B', 'C', 'D', 'E', 'F'};

	public MySimpleTokenizer(String text) {
		this.text = text;
		this.pos = 0;
		next();
	}

	boolean hasCurrent() {
		return current != null;
	}

	Object current() {
		return current;
	}

	public void next() {
		consumewhite();
		if (pos == text.length()) {
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
			int start = pos;
			pos++;
			while (pos < text.length() && (text.charAt(pos) != '\"'))
				pos++;
			pos++;
			current = text.substring(start, pos);
		} else if (text.charAt(pos) == '\'') {
			int start = pos;
			pos++;
			while (pos < text.length() && (text.charAt(pos) != '\''))
				pos++;
			if (pos<text.length()) pos++;
			current = text.substring(start, pos);
		} else if (Character.isDigit(text.charAt(pos))) {
			int start = pos;
			if (pos+1<text.length() && text.charAt(pos+1) == 'x') {
				pos +=2;
				start = pos;
				while (pos < text.length() && (Character.isDigit(text.charAt(pos)) ||
						isin(text.charAt(pos),hexdig)))
					pos++;
				Long w;
				try {
					w = Long.parseLong(text.substring(start,pos), 16);
				} catch (NumberFormatException nf2) {
					w = null;
				}
				current = (Integer) (int) (w & 0xffffffff);
			} else {
			while (pos < text.length() && Character.isDigit(text.charAt(pos)))
				pos++;
			current = Integer.parseInt(text.substring(start, pos));
			}
		} else if (text.charAt(pos) == '-') {
			int start = pos;
			pos++;
			while (pos < text.length() && Character.isDigit(text.charAt(pos)))
				pos++;
			current = Integer.parseInt(text.substring(start, pos));
		} else {
			int start = pos;
			while (pos < text.length() && !isin(text.charAt(pos), symbol)
					&& !isin(text.charAt(pos), whitespace))
				pos++;
			current = text.substring(start, pos);
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
}
