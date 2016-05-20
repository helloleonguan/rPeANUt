import java.io.IOException;
import java.io.PrintStream;


public class MemoryTerm implements Memory {

	private int memory[];
	Simulate simulate;
	
	public MemoryTerm(Simulate simulate) {
		memory = new int[MemoryUI.addressSize];
		this.simulate = simulate;
		reset();
	}
	
	@Override
	public int get(int add) throws MemFaultException {
		if (add == 0xFFF0) {
			
				try {
					if (System.in.available() > 0) {
						int tchar = System.in.read();
						return tchar;
					} else {
						return 0;
					}
				} catch (IOException e) {

					return 0;
				}
			
		}
		if (add == 0xFFF1) {
			
				try {
					return (System.in.available() > 0 ? 0x0001 : 0x0000);
				} catch (IOException e) {
					return (0x0000);

				}
		}

		if (add == 0xFFF2)
			return (simulate.terminalCharInterrupt ? 0x0001 : 0x0000);
		if (add < 0 || add > 0x7FFF)
			throw new MemFaultException();
		
		return memory[add];
		
	}

	@Override
	public void reset() {
	
		for (int i = 0; i < MemoryUI.addressSize; i++) {
			
			memory[i] = 0;
		}
	}

	@Override
	public void set(int add, int value) throws MemFaultException {
		
		if (add == 0xFFF0) {
			
				System.out.print(String.format("%c", 0xFF & value));
				System.out.flush();
			
		} else if (add == 0xFFF2) {
			simulate.terminalCharInterrupt = (value & 0x0001) == 0x0001;
		} else {
			if (add < 0 || add > 0x7FFF)
				throw new MemFaultException();
			
			memory[add] = value;
		}
	}
	
	public void objdump(PrintStream out) {
		for (int i = 0; i < MemoryUI.addressSize; i++) {
			if (memory[i] != 0) {
				out.println(new Address(i) + " " + new Word(memory[i]));
			}
		}
	}

	@Override
	public void setSymbol(int add, String label) {
		
	}

	@Override
	public void resetProfile() {
		
	}


}
