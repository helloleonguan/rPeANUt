import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/*
 rPeanut - is a simple simulator of the rPeANUt computer.
 Copyright (C) 2011-2012  Eric McCreath
 Copyright (C) 2012  Tim Sergeant
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

public class EditCode extends JPanel implements Runnable, AdjustmentListener, ActionListener {
	JScrollPane scroll;
	JTextPane text;
	LineNumbers ln;
	LinkedList<UndoItem> undolist;
	boolean changedFile, textChangedSinceHighlight, autohighlight;
	StyleContext sc;
	MutableAttributeSet aset1, aset2;
	StyledDocument doc;
	ParseErrors errorlist;
	Timer updateTimer;
	MemoryUI nullMemory;
	final Peanut peanut;
//	static String lf = System.getProperty("line.separator");
	static String lf = "\n";
	
	private class UndoItem {
		String text;
		int caretPos;

		private UndoItem(String text, int caretPos) {
			this.text = text;
			this.caretPos = caretPos;
		}
	}

	static final int MAXUNDO = 200;

	public EditCode(int fontsize, Peanut peanut) {
		textChangedSinceHighlight = true;
		errorlist = null;
		this.peanut = peanut;
		nullMemory = new MemoryUI(new Simulate(false,false,false));
		undolist = new LinkedList<UndoItem>();
		text = new JTextPane() {
			@Override
			public void paint(Graphics g) {

				super.paint(g);
				if (errorlist != null && autohighlight) {
					g.setColor(Color.red);
					
					String t;
					try {
						t = text.getDocument().getText(0, text.getDocument().getLength());
					} catch (BadLocationException e1) {
						t = text.getText();
					} 
							
					String str[] = t.split("\n");
					int pos = 0;
					for (int i = 0; i < str.length; i++) {
						if (str[i].length() > 0 && errorlist.hasline(i+1)) {

							try {
								Rectangle r1 = text.getUI().modelToView(text,
										pos);
								Rectangle r2 = text.getUI().modelToView(text,
										pos + str[i].length());
								//g.drawRect(r1.x, r1.y, r2.x - r1.x, r1.height);
								drawWiggle(g,r1.x,r2.x,r1.y+r1.height);

							} catch (BadLocationException e) {
								
								e.printStackTrace();
							}

						}
						pos += str[i].length() + 1;
					}
				}
			}

			private void drawWiggle(Graphics g, int x1, int x2, int y) {
				int x = x1;
				int whu = 3;
				int whd = 0;
				int ws = 4;
				while (x < x2) {
					g.drawLine(x, y-whu, x+ws, y+whd);
					x += ws;
					if (x < x2) {
						g.drawLine(x, y+whd, x+ws, y-whu);
					
					x += ws;
					}
				}
			}
		};
		text.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
		changedFile = false;

		snap();
		Font textfont = new Font(Font.MONOSPACED, Font.PLAIN, fontsize);
		text.setFont(textfont);
		ln = new LineNumbers(textfont);
	
		text.getDocument().addDocumentListener(new DocumentListener() {
			@SuppressWarnings("unused")
			public String getText() {
				int caretPosition = text.getDocument().getLength();
				Element root = text.getDocument().getDefaultRootElement();
				String text = "1" + System.getProperty("line.separator");
				for (int i = 2; i < root.getElementIndex(caretPosition) + 2; i++) {
					text += i + System.getProperty("line.separator");
				}
				//System.out.println("get text: " + text);
				return text;
			}

			@Override
			public void changedUpdate(DocumentEvent de) {
				// snap();
				// ln.setText(getText());

				//System.out.println("changedUpdate");
			}

			@Override
			public void insertUpdate(DocumentEvent de) {
				changedFile = true;
				textChangedSinceHighlight = true;
			
				snap(de);
				
				//System.out.println("insertUpdate");
				// ln.setText(getText());
				// System.out.println("B" + scroll.getVisibleRect());
			}

			@Override
			public void removeUpdate(DocumentEvent de) {
				// snap(de);
				// ln.setText(getText());
				// System.out.println("C");
				//System.out.println("removeUpdate");
				textChangedSinceHighlight = true;
			}
		});
		doc = text.getStyledDocument();

		sc = StyleContext.getDefaultStyleContext();
		aset1 = new SimpleAttributeSet();
		aset2 = new SimpleAttributeSet();
		aset1.addAttribute(StyleConstants.Foreground, Color.black);
		aset2.addAttribute(StyleConstants.Foreground, Color.black);
		StyleConstants.setUnderline(aset1, true);

		/*
		 * text.setHighlighter(new DefaultHighlighter(){ public void
		 * paintLayeredHighlights(Graphics g, int p0, int p1, Shape viewBounds,
		 * JTextComponent editor, View view) { super.paintLayeredHighlights(g,
		 * p0, p1, viewBounds, editor, view); g.setColor(Color.red);
		 * g.drawLine(0,0,10,10);
		 * 
		 * }
		 * 
		 * });
		 */
		/*
		 * aset1 = sc.addAttribute(SimpleAttributeSet.EMPTY,
		 * StyleConstants.Foreground, Color.red); aset2 =
		 * sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground,
		 * Color.black);
		 */

		scroll = new JScrollPane(text);

		// scroll.getViewport().add(text);
		// scroll.setBorder(new EmptyBorder(0,0,0,0));
		// scroll.setRowHeaderView(ln);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		scroll.getVerticalScrollBar().addAdjustmentListener(this);
		this.add(ln, 0);
		this.add(scroll, 1);
		
		updateTimer = new Timer(1000, this);
		updateTimer.setRepeats(true);
		updateTimer.start();
		// this.setMinimumSize(new Dimension(100,100));
		// scroll.setPreferredSize(new Dimension(100,110));
	}

	@Override
	public void doLayout() {
		super.doLayout();
		// Point p = this.getLocation();
		Dimension d = this.getSize();
		int wid = ln.getWid();
		scroll.setSize(new Dimension(d.width - wid, d.height));
		scroll.setLocation(wid, 0);
		ln.setSize(new Dimension(wid, d.height));
		ln.setLocation(0, 0);
	}

	public void save(File file) {
		PrintWriter pr;
		try {
			pr = new PrintWriter(file);
			pr.append(text.getText());
			pr.close();
			changedFile = false;
		} catch (FileNotFoundException e) {
			System.out.println("Problem Finding File : " + file);
		}
	}

	public void load(File file) throws FileNotFoundException, IOException {
		snap();
		text.setText(loadfile(file));
		changedFile = false;

	}

	public static String loadfile(File file) throws FileNotFoundException,
			IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line + lf);
		}
		br.close();
		return (sb.toString());
	}

	public void undo() {
		if (undolist.size() > 1) {
			// System.out.println("undo : " + undolist);
			final UndoItem undoItem = undolist.get(undolist.size() - 2);
			text.setText(undoItem.text);
			try {
				text.setCaretPosition(undoItem.caretPos);
			} catch (Exception e) {
				e.printStackTrace();
			}
			undolist.remove(undolist.size() - 1);
			undolist.remove(undolist.size() - 1);

		}
	}

	public void redo() {
		// still todo
	}

	public void snap() {
		snap(null);
		
	}

	private void snap(DocumentEvent de) {
		int pos = text.getCaretPosition();
		if (de != null) {
			pos += de.getLength();
		}
		 peanut.setTitle();

		undolist.add(new UndoItem(text.getText(), pos));
		if (undolist.size() > MAXUNDO)
			undolist.remove(0);
		// System.out.println("snap : " + undolist);
	}

	public String text() {
		return text.getText();
	}

	public void newtext() {
		text.setText("");
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent arg0) {
		JScrollBar sb = scroll.getVerticalScrollBar();
		ln.setPlace(sb.getModel().getValue(), sb.getModel().getExtent());
		this.revalidate();
	}

	public void highlighterrors(ParseErrors errorlist) {
		this.errorlist = errorlist;
		this.repaint();
	}

	@Override
	public void run() {
		String t = text.getText();
		String str[] = t.split(lf);

		int pos = 0;
		for (int i = 0; i < str.length; i++) {
			if (str[i].length() > 0) {
				if (i % 2 == 0) {
					doc.setCharacterAttributes(pos, str[i].length(), aset1,
							true);
				} else {
					doc.setCharacterAttributes(pos, str[i].length(), aset2,
							true);
				}
				try {
					System.out.println(text.getUI().modelToView(text, 10).x
							+ "");

				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			pos += str[i].length() + 1;
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		if (autohighlight && textChangedSinceHighlight) {
			
		    errorlist = Assemble.assemble(this, nullMemory);
		    this.repaint(); 
		    textChangedSinceHighlight = false;
		}
	}

	public void setAutoHighlight(boolean b) {
		autohighlight = b;
		Assemble.assemble(this, nullMemory);
		this.repaint();
	}

	
	public boolean getAutoHighLight() {
		
		return autohighlight;
	}
}
