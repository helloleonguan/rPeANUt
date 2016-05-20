import java.io.PrintStream;


public interface Memory {
	public int get(int add) throws MemFaultException;
	public void reset();
	public void set(int add, int value) throws MemFaultException;
	public void objdump(PrintStream out);
	public void setSymbol(int add, String label);
	public void resetProfile();
}
