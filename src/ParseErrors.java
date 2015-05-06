import java.util.ArrayList;


public class ParseErrors extends ArrayList<ParseError> {

	public String show() {
		StringBuffer res = new StringBuffer();
		for (ParseError p : this) res.append(p.toString() + "\n");
		return res.toString();
	}
	
	public String show(int limit) {
		StringBuffer res = new StringBuffer();
		for (int i=0;i<size() && i < limit;i++) {
			ParseError p = get(i);
			 res.append(p.toString() + "\n");
		}
		return res.toString();
	}

	public boolean hasline(int i) {
		for (ParseError p : this) if (p.line.linenum() == i) return true;
		return false;
	}
	
	
}
