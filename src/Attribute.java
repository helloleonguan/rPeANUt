/*
rPeanut - is a simple simulator of the rPeANUt computer.
Copyright (C) 2011  Eric McCreath
Copyright (C) 2012  Joshua Worth

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;

public class Attribute {
	AttType type;
	String str;
	String att;
	Integer val;

	Lineinfo li;
	ArrayList<ParseError> errorlist;

	public Attribute(AttType type, String att, String str, Integer val,
			Lineinfo li, ArrayList<ParseError> errorlist) {
		super();
		this.type = type;
		this.att = att;
		this.str = str;
		this.val = val;
		this.li = li;
		this.errorlist = errorlist;
	}
/*	static Attribute parse(String str, Lineinfo li,
			ArrayList<ParseError> errorlist) {
	    Tokenizer tok = new MySimpleTokenizer(str);
	    return parse(tok,li,errorlist);
	}
*/
/*
	static Attribute parse(Tokenizer tok, Lineinfo li,
			ArrayList<ParseError> errorlist) {

		if (!tok.hasCurrent())
			return null;

		if (tok.current().equals("&")) {
			tok.next();
			if (tok.hasCurrent() && tok.current() instanceof String) {
				String lab = (String) tok.current();
				tok.next();
				return new Attribute(AttType.MACROLABEL, "&" + lab, lab, 0, li,
						errorlist);
			} else {
				errorlist.add(new ParseError(li, "String label expected"));
				return null;
			}
		} else if (tok.current().equals("#")) {
			tok.next();
			if (tok.hasCurrent()) {
				if (tok.current() instanceof Integer) {
					Integer w = (Integer) tok.current();
					tok.next();
					return new Attribute(AttType.IVALUE, "#" + w, "" + w, w,
							li, errorlist);
				} else {
					String tstr = (String) tok.current();
					tok.next();
					if (tstr.startsWith("\"")) {
						D.p("parse ISTRING : " + tstr);
						if (tstr.endsWith("\"")) {
							return new Attribute(AttType.ISTRING, tstr,
									tstr.substring(1, tstr.length() - 1), 0,
									li, errorlist);
						} else {
							errorlist.add(new ParseError(li,
									"expecting matching quotes "));
						}
					} else if (tstr.startsWith("'")) {
						D.p("parse IVALUE char : " + tstr);
						if (tstr.length() < 2)
							errorlist
									.add(new ParseError(li,
											"expecting a character after the single quote (note that ; needs escaping)"));
						return new Attribute(AttType.IVALUE, tstr, tstr,
								(int) tstr.charAt(1), li, errorlist);
					} else {
						return new Attribute(AttType.ILABEL, tstr, tstr, null,
								li, errorlist);
					}

				}
			} else {
				errorlist
						.add(new ParseError(li, "expecting something after #"));
			}

		} else {
			if (tok.hasCurrent() && tok.current() instanceof String) {
				String tstr = (String) tok.current();
				tok.next();
				if (tstr.equals("R0") || tstr.equals("R1") || tstr.equals("R2")

				|| tstr.equals("R3") || tstr.equals("R4") || tstr.equals("R5")
						|| tstr.equals("R6") || tstr.equals("R7")
						|| tstr.equals("SP") || tstr.equals("SR")
						|| tstr.equals("PC") || tstr.equals("ONE")
						|| tstr.equals("ZERO") || tstr.equals("MONE")) {
					return new Attribute(AttType.REG, tstr, tstr, null, li,
							errorlist);
				} else if (tstr.startsWith("\"")) {
					D.p("parse STRING : " + tstr);
					if (tstr.endsWith("\"")) {
						return new Attribute(AttType.STRING, tstr,
								tstr.substring(1, tstr.length() - 1), 0, li,
								errorlist);
					} else {
						errorlist.add(new ParseError(li,
								"expecting matching quotes "));
						return null;
					}
				} else {
					return new Attribute(AttType.LABEL, tstr, tstr, null, li,
							errorlist);
				}
			} else { // Integer
				int v = (Integer) tok.current();
				tok.next();
				return new Attribute(AttType.VALUE, v + "", v + "", (int) v,
						li, errorlist);
			}

		}

		return null;
	}
*/
	public int rcode() {
		return Attribute.rcode(str, li, errorlist);
	}

	static public int rcode(String rs, Lineinfo li,
			ArrayList<ParseError> errorlist) {
		if (rs.equals("R0")) {
			return 0;
		} else if (rs.equals("R1")) {
			return 1;
		} else if (rs.equals("R2")) {
			return 2;
		} else if (rs.equals("R3")) {
			return 3;
		} else if (rs.equals("R4")) {
			return 4;
		} else if (rs.equals("R5")) {
			return 5;
		} else if (rs.equals("R6")) {
			return 6;
		} else if (rs.equals("R7")) {
			return 7;
		} else if (rs.equals("SP")) {
			return 8;
		} else if (rs.equals("SR")) {
			return 9;
		} else if (rs.equals("PC")) {
			return 10;
		} else if (rs.equals("ONE")) {
			return 11;
		} else if (rs.equals("ZERO")) {
			return 12;
		} else if (rs.equals("MONE")) {
			return 13;
		}
		errorlist.add(new ParseError(li, rs
				+ " is not a register. Register expected "));
		return 0;
	}
}
