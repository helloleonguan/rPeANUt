import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.JTextPane;
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
	
	private Assemble(Memory memory) {

		this.memory = memory;
		// this.simulate.reset();

		this.curr = 0x0000;
		this.labels = new HashMap<String, Integer>();
		this.symbols = new HashMap<Integer, Symbol>();
		this.macros = new HashMap<String, Macro>();
		this.callstack = new ArrayList<Lineinfo>();
		this.errorlist = new ParseErrors();
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

	public static ParseErrors assemble(JTextPane text, Memory memory) {
		return assemblewithfile(text, memory, null);
	}

	public static ParseErrors assemblewithfile(JTextPane text, Memory memory,
			String filename) {
		Assemble a = new Assemble(memory);
        a.doc = text.getStyledDocument();
		a.assemble(text.getText(), filename);
		a.completeLabels(new Lineinfo("", 1, filename));
		D.p("updating");
		// simulate.update();
		return a.errorlist;
	}

	
	
	private void assemble(String text, String filename) {
		if (filename == null)
			filename = "untitled";

		Lineinfo li = null;
		try {

			@SuppressWarnings("resource")
			Scanner scan = new Scanner(text);
			int linenumber = 1;

			while (scan.hasNextLine()) {
				String aline = scan.nextLine();
				li = new Lineinfo(aline, linenumber, filename);
				D.p("========\n parsing line " + linenumber + " : " + aline);
				try {
					Tokenizer tok = new MySimpleTokenizer(aline);

					if (tok.hasCurrent()) {
						Object firsttoken = tok.current();
						tok.next();

						if (tok.hasCurrent() && tok.current().equals(":")) {
							tok.next();
							String label = firsttoken + "";
							D.p("label : " + label);

							Attribute pl = Attribute
									.parse(label, li, errorlist);
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
							tok.next();
						}

						D.p("instruction to parse : \"" + aline + "\" from "
								+ firsttoken);
						// lets parse the instruction
						if (firsttoken instanceof String) {
							String ins = (String) firsttoken;
							Integer code;
							if ((code = code(ins)) != null) {
								// add, sub, mult, div, mod, and, or, xor
								int r1 = parsereg(tok, li);
								int r2 = parsereg(tok, li);
								int rd = parsereg(tok, li);

								buildSetMove(code, r1, r2, rd, 0);
								
							} else if ((code = codeU(ins)) != null) {
								// neg, not, move
								int r1 = parsereg(tok, li);
								int rd = parsereg(tok, li);
								buildSetMove(0xA, code, r1, rd, 0);
							
							} else if (ins.equals("call")) {
								Attribute a1 = Attribute.parse(tok, li,
										errorlist);
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
								Attribute a1 = Attribute.parse(tok, li,
										errorlist);
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
								int r1 = parsereg(tok, li);
								int jcode = (ins.equals("jumpz") ? 0x1 : (ins
										.equals("jumpn") ? 0x2 : 0x3));

								Attribute a1 = Attribute.parse(tok, li,
										errorlist);
								if (a1.type == AttType.VALUE) {
									buildSetMove(0xA, 0x4, jcode, r1, a1.val);

								} else if (a1.type == AttType.LABEL) {
									symbols.put(curr, new Symbol(a1.str, li));
									buildSetMove(0xA, 0x4, jcode, r1, 0);

								} else {
									parseError(li, "address expected in call");
								}
							} else if (ins.equals("set")) {
								int bit = parsebit(tok, li);
								buildSetMove(0xA, 0x5, 0x1, bit, 0);
							} else if (ins.equals("reset")) {
								int bit = parsebit(tok, li);
								buildSetMove(0xA, 0x5, 0x0, bit, 0);
								
							} else if (ins.equals("push")) {
								int r1 = parsereg(tok, li);
								buildSetMove(0xA, 0x6, 0x0, r1, 0);
								
							} else if (ins.equals("pop")) {
								int r1 = parsereg(tok, li);
								buildSetMove(0xA, 0x6, 0x1, r1, 0);
								
							} else if (ins.equals("rotate")) {
								Attribute a1 = Attribute.parse(tok, li,
										errorlist);
								int r1 = parsereg(tok, li);
								int rd = parsereg(tok, li);
								if (a1.type != AttType.IVALUE
										&& a1.type != AttType.REG)

									parseError(li,
											" rotation requires immediate value or register");
								if (a1.type == AttType.IVALUE) {
									buildSetMove(0xB, 0, r1, rd, a1.val & 0x1f);

								} else {
									buildSetMove(0xE, a1.rcode(), r1, rd, 0);

								}
							} else if (ins.equals("load")) {
								int instcode = 0xC;
								Attribute a1 = Attribute.parse(tok, li,
										errorlist);
								Attribute a2 = Attribute.parse(tok, li,
										errorlist);
								Attribute a3 = null;
								if (tok.hasCurrent())
									a3 = Attribute.parse(tok, li, errorlist);
								if (a1.type == AttType.IVALUE
										&& a2.type == AttType.REG && a3 == null) {
									buildSetMove(0xC, 0x0, 0x0,
													a2.rcode(), a1.val);
								

								} else if (a1.type == AttType.ILABEL
										&& a2.type == AttType.REG && a3 == null) {
									symbols.put(curr, new Symbol(a1.str, li));
									buildSetMove(0xC, 0x0, 0x0,
													a2.rcode(), 0);
									
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
									buildSetMove(instcode, 0x2,
													a1.rcode(), a2.rcode(), 0);
									
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
								Attribute a1 = Attribute.parse(tok, li,
										errorlist);
								Attribute a2 = Attribute.parse(tok, li,
										errorlist);
								Attribute a3 = null;
								if (tok.hasCurrent())
									a3 = Attribute.parse(tok, li, errorlist);
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
								Attribute a1 = Attribute.parse(tok, li,
										errorlist);
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
									buildSetMove(0x0000,0x0000);
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
									Attribute a1 = Attribute.parse(tok, li,
											errorlist);
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
											assemble(ftext, a1.str);
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
								if (!scan.hasNextLine())
									parseError(li, "expected MEND");
								aline = scan.nextLine();
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
										Attribute a1 = Attribute.parse(macp,
												li, errorlist);
										if (a1.type != AttType.MACROLABEL) {
											parseError(li,
													"arguments of first line in macro need to start with &");
										}
										mac.addArgument(a1.str);
									}
									// Read macro body lines
									boolean ended = false;
									while (scan.hasNextLine()) {
										aline = scan.nextLine();
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
									macros.put(mname, mac);
									D.p(macros.keySet().toString());
								}
							} else if (macros.containsKey(ins)) {
								D.p("parsing macro...");
								ArrayList<String> passargs = new ArrayList<String>();
								while (tok.hasCurrent()) {
									Attribute att = Attribute.parse(tok, li,
											errorlist);
									passargs.add(att.att);
								}
								Macro mac = macros.get(ins);
								String mactext = mac.getText(passargs, li,
										errorlist);
								callstack.add(li);
								assemble(mactext, "macro:" + ins);
								callstack.remove(callstack.size() - 1);
								D.p("finished parsing macro");
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
				linenumber++;

			}
		} catch (MemFaultException mfe) {
			parseError(li, "memory out of range (MemFaultException)");
		}

	}

	private void buildSetMove(int instcode, int r1, int r2, int r3, int val)
			throws MemFaultException {
		memory.set(curr, Word.build(instcode, r1, r2, r3, val));
		curr++;
	}

	private void buildSetMove(int v1, int v2)
			throws MemFaultException {
		memory.set(curr, Word.build(v1,v2));
		curr++;
	}
	
	void completeLabels(Lineinfo li) {
		// fill in all the missing symbols (now we know their locations)
		for (Integer add : symbols.keySet()) {
			Symbol label = symbols.get(add);
			Integer val = labels.get(label.symbol);
			if (val == null) {
				parseError( label.li, "unknown symbol : " + label.symbol);
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

	private int parsereg(Tokenizer tok, Lineinfo li) {
		if (!tok.hasCurrent() || !(tok.current() instanceof String))
			parseError(li, "expecting a register");
		String rs = (String) tok.current();
		tok.next();
		return Attribute.rcode(rs, li, errorlist);
	}
}
