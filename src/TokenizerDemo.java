
public class TokenizerDemo {

	static final String prog1 = "0x100 : load #3 R1\n" +
			"load #'H' R2  ; some comments\n" +
			"load #-4 R4  \n" +
			"str : block #\"Hello\"\n";
	public static void main(String[] args) {
		Tokenizer tok = new MySimpleTokenizer(prog1);
		while (tok.hasCurrent()) {
			System.out.println(tok.current());
			tok.next();
		}
	}

}
