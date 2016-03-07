import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

/*
 rPeanut - is a simple simulator of the rPeANUt computer.
 Copyright (C) 2011,2012,2013,2014  Eric McCreath
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

/* 
 * Some ideas for improvement from Nathan Rickerby
 * Okay, here are some of the things that came to mind as I was playing
 with rPeANUt.

 Double clicking on a memory location in the memory window could bring up
 the Poke dialogue with set to that memory address.

 A slider so you can adjust the size of the code window. Or the ability
 to disable it altogether. My workflow was editing with vim, switch to
 rpeanut, load last, assemble, run.

 A slider so you can increase the size of the memory display window. It
 often feels really cramped, you want to be able to make it much bigger
 so you can see more instructions.

 A toggle that makes the memory window scroll to display the currently
 running instruction. Or PC could be a button that when clicked makes the
 memory window jump to the instruction.

 Here's one that's probably a lot of effort. A profiler. An execution
 count for every instruction. It could be another column in the memory
 window. This would emphasise clearly which instructions needed attention
 for optimisation.

 The instruction count displayed somewhere in the rpeanut gui. So that
 people didn't need to use the command line interface to get the count.

 The option of changing the base that the registers are displayed in, or
 displaying them in more than one base (eg. hex and dec).

 Load input from a file when using the GUI. It's possible to do this by
 cutting and pasting into the terminal window, but I found this was a
 little platform dependent. It would be great if there was something you
 could click that loaded input from a file. This would have been useful
 when doing the graphical assignment.

 A pause button that does the same thing as a break point while it's
 running. Have the slow button work during execution, so you can switch
 to slow mode (save setting a break point and resuming with slow).

 A special label: that could be inserted into the code that would
 automatically add a breakpoint to the assembled code. The usefulness of
 this becomes apparent when you're re-adding breakpoints each time you
 assemble.

 An built in instruction set reference.

 Another big effort one. Different execution times for different
 instructions. This doesn't actually mean that some instructions should
 have a delay, but instead of raw instruction count, for every instruction
 add some time value then display the total time at the end instead of
 a total instruction count. The main point of this would be to load and
 store instructions that dealt with memory would be slower than those
 instructions that dealt with registers. A more advanced version of this
 might maintain list of things that would be in a real machines cache
 and only add a large time for cache misses. Then assess based on time,
 rather than instruction count.

 */

public class Peanut implements ActionListener, LayoutManager,
		WindowFocusListener {
	static final String version = "3.1";

	JFrame jframe;

	//JCheckBoxMenuItem echoInputItem, profileItem;
	JCheckBoxMenuItem autohighlighItem;
	JMenuItem pipeMenuItem;
	JMenuItem cacheMenuItem;

	EditCode editcode;
	JButton assembleJButton;
	JButton pokeJButton;
	Simulate simulate;

	JFileChooser jfcs;

	static Preferences prefs = null;

	static final String LOAD = "load";
	static final String LOADLAST = "loadlast";
	static final String SAVE = "save";
	private static final String SAVEAS = "saveas";
	static final String EXIT = "exit";
	static final String NEW = "new";
	static final String ASSEMBLE = "assemble";
	static final String RUN = "run";
	static final String STEP = "step";
	static final String POKE = "poke";
	static final String UNDO = "undo";
	static final String REDO = "redo";
	static final String STOP = "stop";
	static final String SHOWCACHE = "showcache";

	int lastpoke;
	private static final String FONTSIZE = "editfont";
	private static final String FONTUISIZE = "uifont";
	private static final String CHANGEECHO = "changeecho";
	private static final String CHANGEPROFILE = "changeprofile";
	private File currentFileName = null;
	private static final String PIPEFILE = "pipe";

	private static final String CHANGEAUTOHIGHLIGHT = "changeautohighlight";

	public Peanut() {
		lastpoke = 0;
		jframe = new JFrame("rPeANUt - " + version);

		jframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		jframe.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent winEvt) {
				exit();
			}
		});
		// jframe.setExtendedState(jframe.getExtendedState()
		// | JFrame.MAXIMIZED_BOTH);

		prefs = Preferences.userNodeForPackage(Peanut.class);

		JMenuBar bar;
		JMenu fileMenu, editMenu, codeMenu;

		bar = new JMenuBar();

		fileMenu = new JMenu("File");
		fileMenu.setFont(setUIFont(fileMenu.getFont()));
		createMenuItem(fileMenu, "New", NEW,
				KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		createMenuItem(fileMenu, "Load", LOAD,
				KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
		createMenuItem(fileMenu, "Load Last", LOADLAST,
				KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		createMenuItem(fileMenu, "Save", SAVE,
				KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		createMenuItem(fileMenu, "Save As", SAVEAS, KeyStroke.getKeyStroke(
				KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK
						| InputEvent.CTRL_DOWN_MASK));
		createMenuItem(fileMenu, "Exit", EXIT,
				KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));

		editMenu = new JMenu("Edit");
		editMenu.setFont(setUIFont(editMenu.getFont()));
		createMenuItem(editMenu, "Undo", UNDO,
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
		createMenuItem(editMenu, "Change Edit Font Size", FONTSIZE, null);
		createMenuItem(editMenu, "Change UI Font Size", FONTUISIZE, null);

		codeMenu = new JMenu("Code");
		codeMenu.setFont(setUIFont(codeMenu.getFont()));
		createMenuItem(codeMenu, "Assemble", ASSEMBLE, KeyStroke.getKeyStroke(
				KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK));
		createMenuItem(codeMenu, "Run", RUN, KeyStroke.getKeyStroke(
				KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
		createMenuItem(codeMenu, "Step", STEP, KeyStroke.getKeyStroke(
				KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		createMenuItem(codeMenu, "Stop", STOP, KeyStroke.getKeyStroke(
				KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));

		codeMenu.add(new JSeparator());

		createCheckBox(codeMenu, "Echo terminal input", CHANGEECHO,
				prefs.getBoolean("echo", false));
		createCheckBox(codeMenu, "Profile", CHANGEPROFILE,
				prefs.getBoolean("profile", false));

		autohighlighItem = createCheckBox(codeMenu, "Auto Highlight Errors", CHANGEAUTOHIGHLIGHT,
				prefs.getBoolean("autohighlight", false));

		
		createMenuItem(codeMenu, "Input file to terminal", PIPEFILE, null);

		createMenuItem(codeMenu, "Show cache simulator", SHOWCACHE, null);

		bar.add(fileMenu);
		bar.add(editMenu);
		bar.add(codeMenu);

		jframe.setJMenuBar(bar);

		JPanel mainpanel = new JPanel();
		JPanel editpanel = new JPanel();
		editpanel.setLayout(this);
		mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.PAGE_AXIS));

		JSplitPane split = new JSplitPane();
		split.setResizeWeight(1.0); // This means that the edit panel will
									// absorb any extra space given by resizing.
		// mainpanel.setLayout(this);

		editcode = new EditCode(prefs.getInt(FONTSIZE, 16));

		
		assembleJButton = makeButton("Assemble",ASSEMBLE);
		pokeJButton = makeButton("Poke", POKE);

		editpanel.add(assembleJButton);
		editpanel.add(pokeJButton);
		editpanel.add(editcode);
		split.setLeftComponent(editpanel);
		// mainpanel.add(editpanel);

		simulate = new Simulate(false, prefs.getBoolean("echo", false),
				prefs.getBoolean("profile", false));
		split.setRightComponent(simulate);
		simulate.cache.setLocationRelativeTo(jframe);
		jframe.addWindowFocusListener(this);

		mainpanel.add(split);
		editcode.setAutoHighlight(autohighlighItem.isSelected());
		jframe.getContentPane().add(mainpanel);

		jfcs = new JFileChooser(".");

		jframe.pack();
		jframe.setVisible(true);
	}

	private JButton makeButton(String title, String command) {
		// TODO Auto-generated method stub
		JButton jb = new JButton(title);
		jb.setFont(setUIFont(jb.getFont()));
		jb.addActionListener(this);
		jb.setActionCommand(command);
		return jb;
	}

	static public Font setUIFont(Font font) {
		
		//return font.deriveFont(prefs.getInt(FONTUISIZE, 14));
		if (prefs == null) return new Font(Font.MONOSPACED, Font.PLAIN,  14);
		return new Font(Font.MONOSPACED, Font.PLAIN, prefs.getInt(FONTUISIZE, 14));
	}

	private JCheckBoxMenuItem createCheckBox(JMenu menu, String name, String command,
			boolean state) {
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(name);
		item.setFont(setUIFont(item.getFont()));
		item.setState(state);
		item.setActionCommand(command);
		item.addActionListener(this);
		menu.add(item);
		return item;
	}

	private void createMenuItem(JMenu menu, String name, String command,
			KeyStroke ks) {
		JMenuItem mi = new JMenuItem(name);
		mi.setFont(setUIFont(mi.getFont()));
		mi.setActionCommand(command);
		mi.addActionListener(this);
		if (ks != null)
			mi.setAccelerator(ks);
		menu.add(mi);
	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals(EXIT)) {
			exit();
		} else if (ae.getActionCommand().equals(SAVE)) {
			save();
		} else if (ae.getActionCommand().equals(LOAD)) {
			load();
		} else if (ae.getActionCommand().equals(LOADLAST)) {
			loadLast();
		} else if (ae.getActionCommand().equals(ASSEMBLE)) {
			assemble();
		} else if (ae.getActionCommand().equals(POKE)) {
			poke();
		} else if (ae.getActionCommand().equals(NEW)) {
			newtext();
		} else if (ae.getActionCommand().equals(UNDO)) {
			editcode.undo();
		} else if (ae.getActionCommand().equals(REDO)) {
			editcode.redo();
		} else if (ae.getActionCommand().equals(RUN)) {
			simulate.runPush(Simulate.RunSpeed.NORMAL);
		} else if (ae.getActionCommand().equals(STEP)) {
			simulate.stepPush();
		} else if (ae.getActionCommand().equals(STOP)) {
			simulate.stopPush();
		} else if (ae.getActionCommand().equals(FONTSIZE)) {
			changeFont();
		} else if (ae.getActionCommand().equals(FONTUISIZE)) {
			changeUIFont();
		} else if (ae.getActionCommand().equals(CHANGEECHO)) {
			simulate.toggleEcho();
			prefs.putBoolean("echo", simulate.getEchoInput());
		} else if (ae.getActionCommand().equals(CHANGEPROFILE)) {
			simulate.toggleProfile();
			prefs.putBoolean("profile", simulate.getProfile());
		} else if (ae.getActionCommand().equals(CHANGEAUTOHIGHLIGHT)) {
			editcode.setAutoHighlight(autohighlighItem.isSelected());
			prefs.putBoolean("autohighlight", editcode.getAutoHighLight());
		} else if (ae.getActionCommand().equals(SAVEAS)) {
			saveas();
		} else if (ae.getActionCommand().equals(PIPEFILE)) {
			pipeFile();
		} else if (ae.getActionCommand().equals(SHOWCACHE)) {
			simulate.cache.setVisible(true);
		}
	}

	/**
	 * Shows the JFileChooser, which allows the user to select a file which is
	 * appended to the terminal's input string. If the terminal is set to echo
	 * input, it will also be printed to the terminal.
	 */
	private void pipeFile() {
		int res = jfcs.showOpenDialog(jframe);
		if (res == JFileChooser.APPROVE_OPTION) {
			File pipeFile = jfcs.getSelectedFile();
			D.p("Loading file: " + pipeFile);
			try {
				BufferedReader reader = new BufferedReader(new FileReader(
						pipeFile));
				/*
				 * String line; while ((line = reader.readLine()) != null) { //
				 * Process piped line D.p(line); if (simulate.getEchoInput()) {
				 * for (char c : line.toCharArray()) {
				 * simulate.terminalAppend(c); } }
				 * simulate.terminalAppend('\n'); simulate.terminalChar += line
				 * + "\n"; }
				 */
				int c;
				while ((c = reader.read()) != -1) {
					if (simulate.getEchoInput())
						simulate.terminalAppend(c);
					simulate.terminalChar += ((char) c);
				}
				reader.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void changeFont() {
		String input = JOptionPane.showInputDialog("Enter a new font size (current is " + prefs.getInt(FONTSIZE,16)   + ") : ");
		try {
			int size = Integer.parseInt(input);
			final Font font = new Font(Font.MONOSPACED, Font.PLAIN, size);
			editcode.text.setFont(font);
			editcode.ln.font = font;
			simulate.memtable.setFont(font);
			prefs.putInt(FONTSIZE, size);
		} catch (NumberFormatException e) {
		}
	}
	
	private void changeUIFont() {
		String input = JOptionPane.showInputDialog("Enter a UI new font size (current is " + prefs.getInt(FONTUISIZE,14)   + ", note this requires the program to restart for it to take effect) : ");
		try {
			int size = Integer.parseInt(input);
			prefs.putInt(FONTUISIZE, size);
		} catch (NumberFormatException e) {
		}
	}
	

	private void poke() {

		Address address = new Address(lastpoke);
		Word data = new Word(0);
		JPanel panel = new JPanel(new GridLayout(2, 2));
		panel.add(new JLabel("Address:"));
		panel.add(address);
		panel.add(new JLabel("Data:"));
		panel.add(data);
		if (JOptionPane.showConfirmDialog(null, panel) == JOptionPane.OK_OPTION) {
			try {
				simulate.memory.set(address.get(), data.get());
				lastpoke = address.get() + 1;
				simulate.update();
			} catch (MemFaultException e) {
				e.printStackTrace();
			}
		}
	}

	private void assemble() {
		String text = editcode.text();
		simulate.reset();
		ParseErrors errorlist =  Assemble.assemblewithfile(text, simulate.memory,
				currentFileName != null ? currentFileName.getName() : null);
		editcode.highlighterrors(errorlist);
		if (errorlist.size() > 0) {
		    simulate.reset();	
			String err = errorlist.show(10);
			//editcode.highlighterrors(errorlist);
		    JOptionPane.showMessageDialog(null, err);
		} else {
			simulate.update();
		}
	}

	private void exit() {
		simulate.cache.setAlwaysOnTop(false);
		if (editcode.changedFile) {
			Object[] options = { "Save and exit", "Exit without saving",
					"Cancel" };
			int r = JOptionPane
					.showOptionDialog(
							jframe,
							"Are you sure you want to quit? There are unsaved changes.",
							"Are you sure?", JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.WARNING_MESSAGE, null, options,
							options[0]);
			if (r == 0) {
				save();
				System.exit(0);
			} else if (r == 1) {
				System.exit(0);
			}
		} else {
			System.exit(0);
		}
		simulate.cache.setAlwaysOnTop(true);
	}

	private void save() {
		if (currentFileName == null) {
			saveas();
		} else {
			editcode.save(currentFileName);
		}
	}

	private void saveas() {
		int res = jfcs.showSaveDialog(jframe);
		if (res == JFileChooser.APPROVE_OPTION) {
			File file = jfcs.getSelectedFile();
			editcode.save(file);
			prefs.put("lastfile", file.getPath());
			setCurrentFile(file);
		}
	}

	private void newtext() {
		editcode.newtext();
		setCurrentFile(null);
	}

	private void load() {
		int res = jfcs.showOpenDialog(jframe);
		if (res == JFileChooser.APPROVE_OPTION) {
			try {
				File file = jfcs.getSelectedFile();
				editcode.load(file);
				prefs.put("lastfile", file.getPath());
				setCurrentFile(file);
			} catch (FileNotFoundException e) {
				System.out.println("Problem Loading.. " + e);
			} catch (IOException e) {
				System.out.println("Problem Loading.." + e);
			}
		}
	}

	private void setCurrentFile(File file) {
		currentFileName = file;
		if (file != null) {
			jframe.setTitle("rPeANUt - " + version + ": "
					+ currentFileName.getAbsolutePath());
		} else {
			jframe.setTitle("rPeANUt - " + version);
		}
	}

	private void loadLast() {
		String last = prefs.get("lastfile", null);
		if (last != null) {
			try {
				final File file = new File(last);
				editcode.load(file);
				setCurrentFile(file);
			} catch (FileNotFoundException e) {
				System.out.println("Problem Loading.. " + e);
			} catch (IOException e) {
				System.out.println("Problem Loading.." + e);
			}
		}
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			String filename = null;
			boolean dumpframebuffer = false;
			boolean countsteps = false;
			boolean check = false;
			boolean screen = false;
			boolean objdump = false;
			boolean load = false;
			try {
				for (int i = 0; i < args.length; i++) {
					if (args[i].startsWith("-")) {
						if (args[i].equals("-dump")) {
							dumpframebuffer = true;
						} else if (args[i].equals("-count")) {
							countsteps = true;
						} else if (args[i].equals("-check")) {
							check = true;
						} else if (args[i].equals("-screen")) {
							screen = true;
						} else if (args[i].equals("-objdump")) {
							objdump = true;
						} else if (args[i].equals("-load")) {
							load = true;
						} else if (args[i].equals("-help")) {
							printHelpInfo(System.out);
							System.exit(0);
						} else {
							throw new ArgsException("unknown argument : "
									+ args[i]);
						}
					} else {
						if (filename != null)
							throw new ArgsException(
									"Only expecting one filename");
						filename = args[i];
					}
				}
				if (filename == null)
					throw new ArgsException("filename expected");

				if (load) {
					Peanut peanut = new Peanut();
					File file = new File(filename);
					peanut.editcode.load(file);
					peanut.setCurrentFile(file);
				} else {
					String text;

					text = EditCode.loadfile(new File(filename));

					Simulate simulate = new Simulate(true, false, false);

					if (check) {
                        simulate.reset();
						ArrayList<ParseError> errorlist = Assemble.assemble(
								text, simulate.memory);
						if (errorlist.size() > 0) {
							System.out.println("Problem : " + errorlist);
							System.exit(1);
						}
						System.out.println("okay");
						System.exit(0);
					} else {
						simulate.reset();
						ArrayList<ParseError> errorlist = Assemble.assemble(
								text, simulate.memory);
						if (errorlist.size() == 0) {
							if (objdump) {
								simulate.memory.objdump(System.out);
							} else {

								if (screen) {
									JFScreen jscreen = new JFScreen(simulate);
									Thread t = new Thread(jscreen);
									t.start();
								}
								while (!simulate.halt) {
									simulate.step();
								}
								if (dumpframebuffer) {
									simulate.screen.dump(System.out);
								}
								if (countsteps) {
									System.out.println("\nCount : "
											+ simulate.count);
								}
							}
						} else {
							System.err.println(errorlist);
							System.exit(1);
						}
						System.exit(0);
					}
				}
			} catch (ArgsException ae) {
				System.err.println(ae);
				System.exit(1);
			} catch (FileNotFoundException e) {
				System.err.println("Unable to find file : " + filename);
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Problem reading file : " + filename);
				System.exit(1);
			}

		} else {
			new Peanut();
		}
	}

	private static void printHelpInfo(PrintStream out) {
		final String helpstr = "rPeANUt is a RISC version of the PeANUt computer. The simulator is written\n"
				+ "completely in Java at the beginning of 2011. The source code is GPL and \n"
				+ "available in the jar file.\n\n"
				+ "rPeANUt requires a Java Runtime Environment (JRE) or Java Development Kit (JDK)\n"
				+ "version 7 installed on your system (I use OpenJDK Runtime Environment, however,\n"
				+ "other Java implementation would generally also work). Once you have the JRE going\n"
				+ "rPeANUt should run in Windows, Mac, or Linux without too much trouble.\n\n"
				+ "To run the simulator with the GUI simply down load the jar and execute:\n"
				+ "    java -jar rPeANUt"
				+ version
				+ ".jar\n\n"
				+ "To run the simulator from the command-line just execute:\n"
				+ "    java -jar rPeANUt"
				+ version
				+ ".jar <code.s>\n\n"
				+ "rPeANUt has the following command line options:\n"
				+ "    -dump : this does a dump of the frame buffer once the computer halts.\n"
				+ "    -count : this produces a count of the instructions executed once the program halts.\n"
				+ "    -check : this checks whether a script can compile or not\n"
				+ "    -screen : this runs the program with only the display screen\n"
				+ "    -load : this opens the rPeANUt editor with the specified file\n"
				+ "    -objdump : this does a dump of the memory directly after it has assembled the specified program\n"
				+ "    -help : this help info\n";
		out.println(helpstr);
	}

	public void addLayoutComponent(String str, Component comp) {
	}

	public void layoutContainer(Container c) {
		Rectangle b = c.getBounds();
		// Dimension simm = simulate.getPreferredSize();
		Dimension abp = assembleJButton.getPreferredSize();
		// assume b is at 0,0
		int mpoint = b.width;
		// simulate.setLocation(mpoint, 0);
		// simulate.setSize(simm.width, b.height);

		assembleJButton.setLocation(mpoint - abp.width, 0);
		assembleJButton.setSize(abp.width, abp.height);

		pokeJButton.setLocation(0, 0);
		pokeJButton.setSize(abp.width, abp.height);

		editcode.setLocation(0, abp.height);
		editcode.setSize(mpoint, b.height - abp.height);

	}

	public Dimension minimumLayoutSize(Container c) {
		Dimension abm = assembleJButton.getMinimumSize();
		Dimension pbm = pokeJButton.getMinimumSize();
		Dimension simm = simulate.getMinimumSize();
		return new Dimension(abm.width + pbm.width, simm.height);
	}

	public Dimension preferredLayoutSize(Container c) {
		Dimension abp = assembleJButton.getPreferredSize();
		Dimension pbp = pokeJButton.getPreferredSize();
		Dimension simp = simulate.getPreferredSize();
		// return new Dimension(abp.width + pbp.width + simp.width,
		// simp.height);
		return new Dimension(abp.width + pbp.width, simp.height);
	}

	public void removeLayoutComponent(Component comp) {
	}

	@Override
	public void windowGainedFocus(WindowEvent e) {
		D.p("focus gained");
		simulate.cache.setAlwaysOnTop(true);
	}

	@Override
	public void windowLostFocus(WindowEvent e) {
		D.p("focus lost");
		simulate.cache.setAlwaysOnTop(false);
	}

	public static int getUIFontHeight() {
		// TODO Auto-generated method stub
		if (prefs == null) return 12;
		return  prefs.getInt(FONTUISIZE, 14) * 15 / 12;
	}
}
