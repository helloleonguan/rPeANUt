import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

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

public class Assemble {

	Memory memory;
	String filename;
	int curr;
	HashMap<String, Integer> labels;
	HashMap<Integer, Symbol> symbols;
	HashMap<String, Macro> macros;
	ArrayList<Lineinfo> callstack;
	ParseErrors errorlist;
	StyledDocument doc;
	SimpleAttributeSet saLabel, saComment, saRegister, saLiteral,
			saInstruction, saMacro;

	private Assemble(Memory memory) {

		this.memory = memory;
		// this.simulate.reset();

		this.curr = 0x0000;
		this.labels = new HashMap<String, Integer>();
		this.symbols = new HashMap<Integer, Symbol>();
		this.macros = new HashMap<String, Macro>();
		this.callstack = new ArrayList<Lineinfo>();
		this.errorlist = new ParseErrors();

		saLabel = makeAttributeFG(Color.blue);
		saRegister = makeAttributeFG(Color.green);
		saComment = makeAttributeFG(Color.pink);
		saLiteral = makeAttributeFG(Color.red);
		saInstruction = makeAttributeFG(Color.black);
		saMacro = makeAttributeFG(Color.black);

	}

	static SimpleAttributeSet makeAttributeFG(Color col) {
		SimpleAttributeSet res = new SimpleAttributeSet();
		res.addAttribute(StyleConstants.Foreground, col);
		return res;
	}

	static Color darker(Color col) {
		return new Color(col.getRGB()).darker();
	}

	public static void assembleFile(String filename, Memory memory)
			throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();
		assemblewithfile(sb.toString(), memory, filename);
	}

	public static ParseErrors assemble(String text, Memory memory) {
		return assemblewithfile(text, memory, null);
	}

	public static ParseErrors assemblewithfile(String text, Memory memory,
			String filename) {
		Assemble a = new Assemble(memory);
		a.doc = null;
		a.assemble(text, filename);
		a.completeLabels(new Lineinfo("", 1, filename));
		D.p("updating");
		// simulate.update();
		return a.errorlist;
	}

	public static ParseErrors assemble(EditCode ec, Memory memory) {
		return assemblewithfile(ec, memory, null);
	}

	public static ParseErrors assemblewithfile(EditCode ec, Memory memory,
			String filename) {
		Assemble a = new Assemble(memory);
		String text = ec.text.getText();
		if (ec.autohighlight) {
			a.doc = ec.text.getStyledDocument();
		} else {

			ec.text.getStyledDocument().setCharacterAttributes(0,
					text.length(), a.saInstruction, true);
			a.doc = null;
		}
		a.assemble(ec.text.getText(), filename);
		a.completeLabels(new Lineinfo("", 1, filename));
		D.p("updating");
		// simulate.update();
		return a.errorlist;
	}

	private void assemble(String text, String filename) {
		boolean consummednewline = true;
		if (filename == null)
			filename = "untitled";

		Lineinfo li = null;
		try {

			@SuppressWarnings("resource")
			Tokenizer tok = new MySimpleTokenizer(text);
			int linenumber = 1;
			if (doc != null)
				doc.setCharacterAttributes(0, text.length(), saComment, true);
			while (tok.hasCurrent()) {
				consummednewline = false;
				int cstart = tok.currentStart();
				int cend = tok.currentEnd();
				String aline = tok.nextLinePeek();
				li = new Lineinfo(aline, linenumber, filename);
				D.p("========\n parsing line " + linenumber + " : " + aline);
				try {

					if (tok.hasCurrent() && !tok.current().equals("\n")) {
						Object firsttoken = tok.current();
						int ftstart = tok.currentStart();
						int ftend = tok.currentEnd();
						tok.next();

						if (tok.hasCurrent() && tok.current().equals(":")) {
							if (doc != null)
								doc.setCharacterAttributes(cstart, cend
										- cstart, saLabel, true);

							tok.next();
							String label = firsttoken + "";
							D.p("label : " + label);

							Attribute pl = parseAtt(label, li, errorlist);
							if (pl.type == AttType.LABEL) {
								if (labels.get(pl.str) != null)
									parseError(li, "duplicate labels");
								labels.put(pl.str, curr);
							} else if (pl.type == AttType.VALUE) {
								if (curr > pl.val)
									parseError(
											li,
											"going back!! (your code appears to be trying to assemble into memory that has already passed)");

								curr = (pl.val).shortValue();
							} else {
								parseError(li, "label or integer expected");
							}
							firsttoken = tok.current();
							ftstart = tok.currentStart();
							ftend = tok.currentEnd();
							tok.next();
						}

						D.p("instruction to parse : \"" + aline + "\" from "
								+ firsttoken);
						// lets parse the instruction
						consummednewline = false;
						if (firsttoken.equals("\n")) {
							consummednewline = true;
						} else if (firsttoken instanceof String) {
							if (doc != null)
								doc.setCharacterAttributes(ftstart, ftend
										- ftstart, saInstruction, true);

							String ins = (String) firsttoken;
							Integer code;
							if ((code = code(ins)) != null) {
								// add, sub, mult, div, mod, and, or, xor,
								// rotate
								/*
								 * int r1 = parsereg(tok, li); int r2 =
								 * parsereg(tok, li); int rd = parsereg(tok,
								 * li);
								 */

								Attribute r1a = parseAtt(tok, li, errorlist);
								Attribute r2a = parseAtt(tok, li, errorlist);
								Attribute rda = parseAtt(tok, li, errorlist);

								if (r1a != null && r2a != null && rda != null
										&& rda.type == AttType.REG) {

									if (r1a.type == AttType.REG
											&& r2a.type == AttType.REG) {

										buildSetMove(code, r1a.rcode(),
												r2a.rcode(), rda.rcode(), 0);
									} else if (r1a.type == AttType.REG
											&& r2a.type == AttType.IVALUE) {
										buildSetMove(code, r1a.rcode(), 0xE,
												rda.rcode(), r2a.val);
									} else if (r1a.type == AttType.REG
											&& r2a.type == AttType.ILABEL) {
										symbols.put(curr, new Symbol(r2a.str,
												li));
										buildSetMove(code, r1a.rcode(), 0xE,
												rda.rcode(), 0);
									} else if (r2a.type == AttType.REG
											&& r1a.type == AttType.IVALUE) {
										buildSetMove(code, 0xE, r2a.rcode(),
												rda.rcode(), r1a.val);
									} else if (r2a.type == AttType.REG
											&& r1a.type == AttType.ILABEL) {
										symbols.put(curr, new Symbol(r1a.str,
												li));
										buildSetMove(code, 0xE, r2a.rcode(),
												rda.rcode(), 0);
									} else {
										parseError(li,
												"expecting registers (or one immediate value for a source register)");
									}

								} else {

									parseError(li,
											"expecting registers (or one immediate value for a source register)");
								}

							} else if ((code = codeU(ins)) != null) {
								// neg, not, move
								Attribute r1a = parseAtt(tok, li, errorlist);
								Attribute rda = parseAtt(tok, li, errorlist);
								if (r1a != null && rda != null
										&& r1a.type == AttType.REG
										&& rda.type == AttType.REG) {

									buildSetMove(0xA, code, r1a.rcode(),
											rda.rcode(), 0);
								} else if (r1a != null && rda != null
										&& r1a.type == AttType.IVALUE
										&& rda.type == AttType.REG) {
									buildSetMove(0xA, code, 0xE, rda.rcode(),
											r1a.val);

								} else if (r1a != null && rda != null
										&& r1a.type == AttType.ILABEL
										&& rda.type == AttType.REG) {
									buildSetMove(0xA, code, 0xE, rda.rcode(),
											r1a.val);
								} else {
									parseError(li,
											"expecting registers (or n immediate value for a source register)");
								}
							} else if (ins.equals("call")) {
								Attribute a1 = parseAtt(tok, li, errorlist);
								if (a1.type == AttType.VALUE) {
									buildSetMove(0xA300, a1.val);
								} else if (a1.type == AttType.LABEL) {
									symbols.put(curr, new Symbol(a1.str, li));
									buildSetMove(0xA300, 0);
								} else {
									parseError(li, "address expected in call");
								}
							} else if (ins.equals("return")) {
								buildSetMove(0xA301, 0);
							} else if (ins.equals("trap")) {
								buildSetMove(0xA302, 0);
							} else if (ins.equals("jump")) {
								Attribute a1 = parseAtt(tok, li, errorlist);
								if (a1.type == AttType.VALUE) {
									buildSetMove(0xA400, a1.val);

								} else if (a1.type == AttType.LABEL) {
									symbols.put(curr, new Symbol(a1.str, li));
									buildSetMove(0xA400, 0);

								} else {
									parseError(li, "address expected in call");
								}
							} else if (ins.equals("jumpz")
									|| ins.equals("jumpn")
									|| ins.equals("jumpnz")) {

								Attribute r1a = parseAtt(tok, li, errorlist);
								if (r1a != null && r1a.type == AttType.REG) {
									int jcode = (ins.equals("jumpz") ? 0x1
											: (ins.equals("jumpn") ? 0x2 : 0x3));

									Attribute a1 = parseAtt(tok, li, errorlist);
									if (a1.type == AttType.VALUE) {
										buildSetMove(0xA, 0x4, jcode,
												r1a.rcode(), a1.val);

									} else if (a1.type == AttType.LABEL) {
										symbols.put(curr,
												new Symbol(a1.str, li));
										buildSetMove(0xA, 0x4, jcode,
												r1a.rcode(), 0);

									} else {
										parseError(li,
												"address expected in call");
									}
								} else {
									parseError(li, " jump requires a register");
								}

							} else if (ins.equals("set")) {
								int bit = parsebit(tok, li);
								buildSetMove(0xA, 0x5, 0x1, bit, 0);
							} else if (ins.equals("reset")) {
								int bit = parsebit(tok, li);
								buildSetMove(0xA, 0x5, 0x0, bit, 0);

							} else if (ins.equals("push")) {

								Attribute r1a = parseAtt(tok, li, errorlist);
								if (r1a != null && r1a.type == AttType.REG) {
									buildSetMove(0xA, 0x6, 0x0, r1a.rcode(), 0);
								} else {
									parseError(li, " push requires a register");
								}

							} else if (ins.equals("pop")) {
								Attribute r1a = parseAtt(tok, li, errorlist);
								if (r1a != null && r1a.type == AttType.REG) {
									buildSetMove(0xA, 0x6, 0x1, r1a.rcode(), 0);
								} else {
									parseError(li, " pop requires a register");
								}

								/*
								 * if (a1.type != AttType.IVALUE && a1.type !=
								 * AttType.REG)
								 * 
								 * parseError(li,
								 * " rotation requires immediate value or register"
								 * ); if (a1.type == AttType.IVALUE) {
								 * buildSetMove(0xB, 0, r1, rd, a1.val & 0x1f);
								 * 
								 * } else { buildSetMove(0xE, a1.rcode(), r1,
								 * rd, 0);
								 * 
								 * }
								 */
							} else if (ins.equals("load")) {
								int instcode = 0xC;
								Attribute a1 = parseAtt(tok, li, errorlist);
								Attribute a2 = parseAtt(tok, li, errorlist);
								Attribute a3 = null;
								if (tok.hasCurrent()
										&& !(tok.current().equals("\n")))
									a3 = parseAtt(tok, li, errorlist);
								if (a1.type == AttType.IVALUE
										&& a2.type == AttType.REG && a3 == null) {
									buildSetMove(0xC, 0x0, 0x0, a2.rcode(),
											a1.val);

								} else if (a1.type == AttType.ILABEL
										&& a2.type == AttType.REG && a3 == null) {
									symbols.put(curr, new Symbol(a1.str, li));
									buildSetMove(0xC, 0x0, 0x0, a2.rcode(), 0);

								} else if (a1.type == AttType.LABEL
										&& a2.type == AttType.REG && a3 == null) {
									symbols.put(curr, new Symbol(a1.str, li));
									buildSetMove(instcode, 0x1, 0x0,
											a2.rcode(), 0);

								} else if (a1.type == AttType.VALUE
										&& a2.type == AttType.REG && a3 == null) {
									buildSetMove(instcode, 0x1, 0x0,
											a2.rcode(), a1.val);

								} else if (a1.type == AttType.REG
										&& a2.type == AttType.REG && a3 == null) {
									buildSetMove(instcode, 0x2, a1.rcode(),
											a2.rcode(), 0);

								} else if (a1.type == AttType.REG
										&& (a2.type == AttType.IVALUE || a2.type == AttType.ILABEL)
										&& a3 != null && a3.type == AttType.REG) {

									if (a2.type == AttType.IVALUE) {
										buildSetMove(instcode, 0x3, a1.rcode(),
												a3.rcode(), a2.val);

									} else {
										symbols.put(curr,
												new Symbol(a2.str, li));
										buildSetMove(instcode, 0x3, a1.rcode(),
												a3.rcode(), 0);

									}

								} else {
									parseError(li, "unknown " + ins
											+ " attributes ");
								}
							} else if (ins.equals("store")) {
								int instcode = 0xD;
								Attribute a1 = parseAtt(tok, li, errorlist);
								Attribute a2 = parseAtt(tok, li, errorlist);
								Attribute a3 = null;
								if (tok.hasCurrent()
										&& !(tok.current().equals("\n")))
									a3 = parseAtt(tok, li, errorlist);
								if ((a2.type == AttType.IVALUE || a2.type == AttType.ILABEL)
										&& a3 == null) {
									parseError(li,
											"you can't store an immediate ");

								} else if (a2.type == AttType.LABEL
										&& a1.type == AttType.REG && a3 == null) {
									symbols.put(curr, new Symbol(a2.str, li));
									buildSetMove(instcode, 0x1, a1.rcode(),
											0x0, 0);

								} else if (a2.type == AttType.VALUE
										&& a1.type == AttType.REG && a3 == null) {
									buildSetMove(instcode, 0x1, a1.rcode(),
											0x0, a2.val);

								} else if (a1.type == AttType.REG
										&& a2.type == AttType.REG && a3 == null) {
									buildSetMove(instcode, 0x2, a1.rcode(),
											a2.rcode(), 0);
								} else if (a1.type == AttType.REG
										&& (a2.type == AttType.IVALUE || a2.type == AttType.ILABEL)
										&& a3 != null && a3.type == AttType.REG) {

									if (a2.type == AttType.IVALUE) {
										buildSetMove(instcode, 0x3, a1.rcode(),
												a3.rcode(), a2.val);

									} else {
										symbols.put(curr,
												new Symbol(a2.str, li));
										buildSetMove(instcode, 0x3, a1.rcode(),
												a3.rcode(), 0);

									}
								} else {
									parseError(li, "unknown " + ins
											+ " attributes ");
								}
							} else if (ins.equals("halt")) {
								buildSetMove(0x0000, 0x0000);
							} else if (ins.equals("block")) {
								Attribute a1 = parseAtt(tok, li, errorlist);
								if (a1.type == AttType.VALUE) {
									for (int i = 0; i < a1.val; i++) {
										buildSetMove(0x0000, 0x0000);
									}
								} else if (a1.type == AttType.IVALUE) {
									memory.set(curr, a1.val);
									curr++;
								} else if (a1.type == AttType.ISTRING) {
									for (int i = 0; i < a1.str.length(); i++) {
										buildSetMove(0, a1.str.charAt(i));
									}
									buildSetMove(0x0000, 0x0000);
								} else if (a1.type == AttType.ILABEL) {
									symbols.put(curr, new Symbol(a1.str, li));
									// memory is set when symbols resolved
									curr++;
								} else {
									parseError(li, "unknown " + ins
											+ " attributes ");
								}
							} else if (ins.equals("#")) {
								if (tok.hasCurrent()
										&& tok.current().equals("include")) {
									tok.next();
									Attribute a1 = parseAtt(tok, li, errorlist);
									if (a1.type == AttType.STRING) {
										try {
											FileInputStream fis = new FileInputStream(
													a1.str);
											int len = fis.available();
											byte[] fdata = new byte[len];
											fis.read(fdata);
											String ftext = new String(fdata, 0,
													len);
											callstack.add(li);
											StyledDocument ordoc = doc;
											doc = null;
											assemble(ftext, a1.str);
											doc = ordoc;
											callstack
													.remove(callstack.size() - 1);
											fis.close();
										} catch (FileNotFoundException e) {
											parseError(
													li,
													"include: file not found '"
															+ a1.str
															+ "'\nNote that the current working directory is "
															+ System.getProperty("user.dir"));
										} catch (Exception e) {
											e.printStackTrace();
											parseError(li,
													"include: error loading file '"
															+ a1.str + "'");
										}
									} else {
										parseError(li,
												"argument of include should be string");
									}
								} else {
									parseError(li, "expecting #include");
								}
							} else if (ins.equals("mend")) {
								parseError(li, "MEND without MACRO");
							} else if (ins.toLowerCase().equals("macro")) {
								StyledDocument ordoc = doc;
								doc = null;
								int macpspos = tok.currentStart();
								if (!tok.hasCurrent())
									parseError(li, "expected MEND");
								aline = tok.nextLine();
								linenumber++;
								li = new Lineinfo(aline, linenumber, filename);
								Tokenizer macp = new MySimpleTokenizer(aline);
								// Get macro name
								if (!macp.hasCurrent()
										|| !(macp.current() instanceof String)) {
									parseError(li, "expecting a macro name");
								} else {
									String mname = ((String) macp.current())
											.toLowerCase();
									macp.next();
									if (mname.toLowerCase().equals("mend")) {
										parseError(li,
												"macros must at least have a type line");
									}
									Macro mac = new Macro();
									// Read macro arguments
									while (macp.hasCurrent()) {
										Attribute a1 = parseAtt(macp, li,
												errorlist);
										if (a1.type != AttType.MACROLABEL) {
											parseError(li,
													"arguments of first line in macro need to start with &");
										}
										mac.addArgument(a1.str);
									}
									// Read macro body lines
									boolean ended = false;
									while (tok.hasCurrent()) {
										aline = tok.nextLine();
										linenumber++;
										macp = new MySimpleTokenizer(aline);
										if (macp.hasCurrent()
												&& macp.current() instanceof String
												&& ((String) macp.current())
														.toLowerCase().equals(
																"mend")) {
											ended = true;
											break;
										}
										mac.addLine(aline);
									}
									if (!ended)
										parseError(li, "expected MEND");
									if (ordoc != null)
										ordoc.setCharacterAttributes(macpspos,
												tok.currentEnd() - macpspos,
												saMacro, true);
									macros.put(mname, mac);
									D.p(macros.keySet().toString());
								}
								doc = ordoc;
							} else if (macros.containsKey(ins)) {
								D.p("parsing macro...");
								StyledDocument ordoc = doc;
								doc = null;
								ArrayList<String> passargs = new ArrayList<String>();
								int acout = 0;
								Macro mac = macros.get(ins);
								while (tok.hasCurrent()
										&& !tok.current().equals("\n")
										&& acout < mac.argcount()) {
									Attribute att = parseAtt(tok, li, errorlist);
									passargs.add(att.att);
									acout++;
								}

								String mactext = mac.getText(passargs, li,
										errorlist);
								callstack.add(li);
								assemble(mactext, "macro:" + ins);
								callstack.remove(callstack.size() - 1);
								D.p("finished parsing macro");
								doc = ordoc;
							} else if (ins.equals("\n")) {
								// we don't need to do anything
							} else {
								parseError(li, "unknown instruction : " + ins);
							}
						}
					}
				} catch (NullPointerException npe) {
					if (D.debug)
						npe.printStackTrace();
					parseError(li,
							"assembler expecting something that is missing??? (NullPointerException)");
				}
				if (!consummednewline) {
					if (tok.hasCurrent() && !(tok.current().equals("\n"))) {
						parseError(li, "expected an end of line");
					} else {
						tok.next();
					}
				}
				linenumber++;

			}
		} catch (MemFaultException mfe) {
			parseError(li, "memory out of range (MemFaultException)");
		}

	}

	private Attribute parseAtt(String str, Lineinfo li,
			ArrayList<ParseError> errorlist) {
		StyledDocument ordoc = doc;
		doc = null;
		Tokenizer tok = new MySimpleTokenizer(str);
		Attribute res = parseAtt(tok, li, errorlist);
		doc = ordoc;
		return res;
	}

	private Attribute parseAtt(Tokenizer tok, Lineinfo li,
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
			int tstart = tok.currentStart();

			tok.next();
			int tend = tok.currentEnd();
			if (tok.hasCurrent()) {
				if (tok.current() instanceof Integer) {
					Integer w = (Integer) tok.current();
					tok.next();
					if (doc != null)
						doc.setCharacterAttributes(tstart, tend - tstart,
								saLiteral, true);
					return new Attribute(AttType.IVALUE, "#" + w, "" + w, w,
							li, errorlist);
				} else {
					String tstr = (String) tok.current();
					tok.next();
					if (tstr.startsWith("\"")) {
						D.p("parse ISTRING : " + tstr);
						if (tstr.endsWith("\"")) {
							if (doc != null)
								doc.setCharacterAttributes(tstart, tend
										- tstart, saLiteral, true);
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
						if (doc != null)
							doc.setCharacterAttributes(tstart, tend - tstart,
									saLiteral, true);
						return new Attribute(AttType.IVALUE, tstr, tstr,
								(int) tstr.charAt(1), li, errorlist);
					} else {
						if (doc != null)
							doc.setCharacterAttributes(tstart, tend - tstart,
									saLabel, true);
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
				int tstart = tok.currentStart();
				int tend = tok.currentEnd();
				tok.next();
				if (tstr.equals("R0") || tstr.equals("R1") || tstr.equals("R2")

				|| tstr.equals("R3") || tstr.equals("R4") || tstr.equals("R5")
						|| tstr.equals("R6") || tstr.equals("R7")
						|| tstr.equals("SP") || tstr.equals("SR")
						|| tstr.equals("PC") || tstr.equals("ONE")
						|| tstr.equals("ZERO") || tstr.equals("MONE")) {
					if (doc != null)
						doc.setCharacterAttributes(tstart, tend - tstart,
								saRegister, true);
					return new Attribute(AttType.REG, tstr, tstr, null, li,
							errorlist);
				} else if (tstr.startsWith("\"")) {
					D.p("parse STRING : " + tstr);
					if (tstr.endsWith("\"")) {
						if (doc != null)
							doc.setCharacterAttributes(tstart, tend - tstart,
									saLiteral, true);
						return new Attribute(AttType.STRING, tstr,
								tstr.substring(1, tstr.length() - 1), 0, li,
								errorlist);
					} else {
						errorlist.add(new ParseError(li,
								"expecting matching quotes "));
						return null;
					}
				} else {
					if (doc != null)
						doc.setCharacterAttributes(tstart, tend - tstart,
								saLabel, true);
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

	private void buildSetMove(int instcode, int r1, int r2, int r3, int val)
			throws MemFaultException {
		memory.set(curr, Word.build(instcode, r1, r2, r3, val));
		curr++;
	}

	private void buildSetMove(int v1, int v2) throws MemFaultException {
		memory.set(curr, Word.build(v1, v2));
		curr++;
	}

	void completeLabels(Lineinfo li) {
		// fill in all the missing symbols (now we know their locations)
		for (Integer add : symbols.keySet()) {
			Symbol label = symbols.get(add);
			Integer val = labels.get(label.symbol);
			if (val == null) {
				parseError(label.li, "unknown symbol : " + label.symbol);
			} else {
				int inst;
				try {
					inst = memory.get(add);
					memory.set(add, inst | val);
				} catch (MemFaultException e) {
					parseError(label.li, "problem setting symbol "
							+ label.symbol + " at " + add);

				}
			}
		}

		for (String lab : labels.keySet()) {
			Integer val = labels.get(lab);
			memory.setSymbol(val, lab);
		}

		// Stop recursive compiles from adding the same label more than once
		// labels.clear();
		memory.resetProfile();
	}

	private void parseError(Lineinfo li, String string) {
		errorlist.add(new ParseError(li, string));
	}

	private int parsebit(Tokenizer inss, Lineinfo li) {
		if (!inss.hasCurrent()) {
			parseError(li, "expecting a bit eg IM OF, or TI");
			return 0;
		}
		if (!(inss.current() instanceof String)) {
			parseError(li, "expecting a bit eg IM OF, or TI");
			return 0;
		}
		String rs = (String) inss.current();
		inss.next();
		if (rs.equals("OF")) {
			return 0x0;
		} else if (rs.equals("IM")) {
			return 0x1;
		} else if (rs.equals("TI")) {
			return 0x2;
		}
		parseError(li, "unknown bit : " + rs);
		return 0;
	}

	private static Integer code(String ins) {

		if (ins.equals("add")) {
			return 0x1;
		} else if (ins.equals("sub")) {
			return 0x2;
		} else if (ins.equals("mult")) {
			return 0x3;
		} else if (ins.equals("div")) {
			return 0x4;
		} else if (ins.equals("mod")) {
			return 0x5;
		} else if (ins.equals("and")) {
			return 0x6;
		} else if (ins.equals("or")) {
			return 0x7;
		} else if (ins.equals("xor")) {
			return 0x8;
		} else if (ins.equals("rotate")) {
			return 0xE;
		} else {
			return null;
		}
	}

	private Integer codeU(String ins) {
		if (ins.equals("neg")) {
			return 0x0;
		} else if (ins.equals("not")) {
			return 0x1;
		} else if (ins.equals("move")) {
			return 0x2;
		} else {
			return null;
		}
	}

	/*
	 * private int parsereg(Tokenizer tok, Lineinfo li) { if (!tok.hasCurrent()
	 * || !(tok.current() instanceof String)) parseError(li,
	 * "expecting a register"); String rs = (String) tok.current(); if (doc !=
	 * null) doc.setCharacterAttributes(tok.currentStart(), tok.currentEnd() -
	 * tok.currentStart(), saRegister, true); tok.next(); return
	 * Attribute.rcode(rs, li, errorlist); }
	 */
}
