package snaq.util;

/**
 * Interface for an object that can be reused.
 * @see snaq.util.ObjectPool
 * @author Giles Winstanley
 */
public interface Reusable
{
	/**
	 * Cleans an object to put it in a state in which it can be reused.
	 * @throws Exception if unable to recycle the this object
	 */
	public void recycle() throws Exception;
}