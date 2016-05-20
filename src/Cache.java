
public interface Cache {
	public int get(int addr) throws MemFaultException;
	public void set(int addr, int val) throws MemFaultException;
	public void reset();
}
