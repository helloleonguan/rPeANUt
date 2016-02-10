import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;


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


public class AssembleTest {

	public void testComp(String str, int s1, int s2) throws MemFaultException {
		Simulate sim = new Simulate(true, false,false);
		sim.reset();
		Assemble.assemble(str, sim.memory);
		assertEquals((s1 << 16) | s2 ,sim.memory.get( 0));

		sim = new Simulate(false, false, false);
		sim.reset();
		Assemble.assemble(str, sim.memory);
		assertEquals((s1 << 16) | s2 ,sim.memory.get( 0));


	}

	public void testCompA(String str, int add, int s1, int s2) throws MemFaultException {
		Simulate sim = new Simulate(true, false,false);
		sim.reset();
		Assemble.assemble(str, sim.memory);
		
		assertEquals((s1 << 16) | s2 ,sim.memory.get( add));

		sim = new Simulate(false, false,false);
		sim.reset();
		Assemble.assemble(str, sim.memory);
		assertEquals((s1 << 16) | s2 ,sim.memory.get( add));
	}

	public void testCompFiles(String filename, String filenameequiv) throws MemFaultException, IOException {
		Simulate sim = new Simulate(true, false,false);
		sim.reset();
		Assemble.assembleFile(filename, sim.memory);
		
		Simulate simequiv = new Simulate(true, false,false);
		simequiv.reset();
		Assemble.assembleFile(filenameequiv, simequiv.memory);
		
		for (int i = 0; i < Memory.addressSize; i++) {
			assertEquals(sim.memory.get(i), simequiv.memory.get(i));
		}
	}

	@Test
	public void testUnitComp() throws MemFaultException {
		testComp("add R1 R2 R3",  0x1123, 0x0000 );
		testComp("sub R4 R2 SP",  0x2428, 0x0000 );
		testComp("mult R4 SR SP", 0x3498, 0x0000 );
		testComp("div R4 R2 R0",  0x4420, 0x0000 );
		testComp("mod R4 R2 R7",  0x5427, 0x0000 );
		testComp("and R4 R2 R1",  0x6421, 0x0000 );
		testComp("add R1 #0x0F R3",  0x11e3, 0x000F );
		testComp("sub #-1 R2 SP",  0x2e28, 0xFFFF );
		testComp("mult R4 #3 SP", 0x34e8, 0x0003 );
		testComp("div R4 #5 R0",  0x44e0, 0x0005 );
		testComp("mod #-1 R2 R7",  0x5e27, 0xFFFF );
		testComp("and #0 R2 R1",  0x6e21, 0x0000 );
		testComp("or R1 R3 R5",   0x7135, 0x0000 );
		testComp("xor R1 R3 R5",   0x08135, 0x0000 );
		testComp("neg R1 R5",   0xA015, 0x0000 );
		testComp("neg #1 R5",   0xA0e5, 0x0001 );
		testComp("not R1 R5",   0xA115, 0x0000 );
		testComp("move R1 R5",   0xA215, 0x0000 );
		testComp("call 0x1234 ",   0xA300, 0x1234 );
		testCompA("0x0005 :  call 0x0005 ",   0x0005, 0xA300, 0x0005 );
		
		testCompA("0x0005 : \n line : call line ",   0x0005, 0xA300, 0x0005 );
		testCompA("0x1321 : \n line : call line ",   0x1321, 0xA300, 0x1321 );
		testCompA("0x4321 : \n line : call line ",   0x4321, 0xA300, 0x4321 );
		testComp("return",   0xA301, 0x0000 );
		testComp("trap",   0xA302, 0x0000 );
		testComp("jump 0x1234 ",   0xA400, 0x1234 );
		testCompA("0x4321 : \n line : jump line ",   0x4321, 0xA400, 0x4321 );
		testComp("jumpz R3 0x1234 ",   0xA413, 0x1234 );
		testCompA("0x4321 : \n line : jumpz R3 line ",   0x4321, 0xA413, 0x4321 );
		testComp("reset OF",   0xA500, 0x0000 );
		testComp("set IM",   0xA511, 0x0000 );
		testComp("push R5",   0xA605, 0x0000 );
		testComp("pop R5",   0xA615, 0x0000 );
		testComp("rotate #3 R1 R2",   0xEE12, 0x0003 );
		testComp("rotate R7  #-1 R2",   0xE7E2, 0xFFFF );
		testComp("rotate R5 R1 R2",   0xE512, 0x0000 );
		testComp("load #0xABCD R5",   0xC005, 0xABCD );
		testComp("load 0xABCD R5",   0xC105, 0xABCD );
		testComp("load R1 R5",   0xC215, 0x0000 );
		testComp("load R1 #0xABCD R5",   0xC315, 0xABCD );
		testComp("store R5 0xABCD",   0xD150, 0xABCD );
		testComp("store R1 R5",   0xD215, 0x0000 );
		testComp("store R1 #0xABCD R5",   0xD315, 0xABCD );
		testComp("halt",   0x0000, 0x0000 );
	}
	
	@Test
	public void testUnitInclude() throws MemFaultException, IOException {
		testCompFiles("tests/testinclude.rp", "tests/testincludeout.rp");
	}

	@Test
	public void testUnitMacro1() throws MemFaultException, IOException {
		testCompFiles("tests/macro.rp", "tests/macroout.rp");
	}
	
	@Test
	public void testUnitMacro2() throws MemFaultException, IOException {
		testCompFiles("tests/macrobug.rp", "tests/macrobugout.rp");
	}
	
	
	void calctest(String inst, int v1, int v2, int res) {
		Simulate sim = new Simulate(false, false,false);
		sim.reset();
		Assemble.assemble("0x0100 : " + inst + " R1 R2 R3", sim.memory);
		sim.r[1].set(v1);
		sim.r[2].set(v2);
		sim.r[3].set(0);
		sim.step();
		assertEquals(res,sim.r[3].get());
	}

	void calctest1(String inst, int v1, int res)  {
		Simulate sim = new Simulate(false, false,false);
		sim.reset();
		Assemble.assemble("0x0100 : " + inst + " R1 R2", sim.memory);
		sim.r[1].set(v1);
 
		sim.r[2].set(0);
		sim.step();
		assertEquals(res,sim.r[2].get());
	}

	@Test
	public void testUnitExecution()  {
		calctest("add",3,7,10);
		calctest("sub",3,7,-4  );
		calctest("mult",3,7,21);
		calctest("div",30,7,4);
		calctest("mod",30,7,2);
		calctest("rotate",2,1,4);
		calctest("and",0xf01a,0x001f,0x001a);
		calctest("or",0xf01a,0x001f,0xf01f);
		calctest("xor",0xf01a,0x001f,0xf005);

		calctest1("neg",-4,4);
		calctest1("not",0x00005f01,0xffffa0fe);
		calctest1("move",0xf32b,0xf32b);
	}
	
	@Test
	public void testTokenizer() {
		testtok("0x100 : load #3 R1", new Object[] {new Integer(256), ":", "load", "#", new Integer(3), "R1"});
		testtok("  label : ; comment", new Object[] {"label", ":"});
		testtok("load    #'H'   R2  ",  new Object[] {"load", "#", "'H'", "R2"});
		testtok("load    #'\n'   R2  ",  new Object[] {"load", "#", "'\n'", "R2"});
		testtok("block    #\"hello\\n\"   ",  new Object[] {"block", "#", "\"hello\n\""});

	}

	private void testtok(String string, Object[] objects) {
		Defines defines = new Defines();
		Tokenizer tok = new MySimpleTokenizer(defines, string);
		int pos = 0;
		while (tok.hasCurrent() && pos < objects.length) {
			assertEquals("mismatch token " + pos,tok.current(),objects[pos]);
			tok.next();
			pos++;
		}
		assertEquals("still more tokens",tok.hasCurrent(), false);
		assertEquals("not enough tokens", pos,objects.length);
	}
	
}
