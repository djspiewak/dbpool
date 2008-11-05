/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.util;

import java.util.EventListener;

/**
 * Listener interface for ObjectPoolEvent objects.
 * Listeners should ensure the implementations of the listed methods return
 * quickly. Tasks that require more time should spawn a new thread.
 * @author  Giles Winstanley
 */
public interface ObjectPoolListener extends EventListener
{
	/**
	 * Called when an item is checked out of the pool.
	 */
	public void poolCheckOut(ObjectPoolEvent evt);
	/**
	 * Called when an item is checked back in to the pool.
	 */
	public void poolCheckIn(ObjectPoolEvent evt);
	/**
	 * Called when a check-out request causes the poolSize limit to be reached.
	 */
	public void maxPoolLimitReached(ObjectPoolEvent evt);
	/**
	 * Called when a check-out request causes the poolSize limit to be exceeded.
	 */
	public void maxPoolLimitExceeded(ObjectPoolEvent evt);
	/**
	 * Called when a check-out request causes the maxSize limit to be reached.
	 */
	public void maxSizeLimitReached(ObjectPoolEvent evt);
	/**
	 * Called when a check-out request attempts to exceed the maxSize limit.
	 */
	public void maxSizeLimitError(ObjectPoolEvent evt);
	/**
	 * Called when the pool's parameters are changed.
	 */
	public void poolParametersChanged(ObjectPoolEvent evt);
	/**
	 * Called when the pool is released (no more events are fired by the pool after this event).
	 */
	public void poolReleased(ObjectPoolEvent evt);
}