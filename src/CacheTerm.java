
public class CacheTerm implements Cache {
    Memory memory;
	public CacheTerm(Memory mem) {
		memory = mem;
	}

	@Override
	public int get(int addr) throws MemFaultException {
		
		return memory.get(addr);
	}

	@Override
	public void set(int addr, int val) throws MemFaultException {
		memory.set(addr, val);
	}

	@Override
	public void reset() {
	
	}

}
