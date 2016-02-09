
public class TokenizerDemo {

	static final String prog1 = "#'\\n'  0x100 : load #3 R1\n" +
			"load #'H' R2  ; some comments\n" +
			"load #-4 R4  \n" +
			"str : block #\"Hello\"\n" +
			   "load #'\n' R4\n" +
				"str : block #\"Hello\\n\"\n";
	public static void main(String[] args) {
		Tokenizer tok = new MySimpleTokenizer(prog1);
		while (tok.hasCurrent()) {
			System.out.println("tok : ["  + tok.current() + "]");
			tok.next();
		}
	}

}
