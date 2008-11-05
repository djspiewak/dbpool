/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import snaq.util.ObjectPoolListener;

/**
 * Listener for ConnectionPoolEvent objects.
 * Listeners should ensure the implementations of the listed methods return
 * quickly. Tasks that require more time should spawn a new thread.
 * @author Giles Winstanley
 */
public interface ConnectionPoolListener extends ObjectPoolListener
{
	/**
	 * Called when a connection is found to be invalid.
	 */
	public void validationError(ConnectionPoolEvent evt);
	/**
	 * Called when a connection is checked out of the pool.
	 */
	public void poolCheckOut(ConnectionPoolEvent evt);
	/**
	 * Called when a connection is checked back in to the pool.
	 */
	public void poolCheckIn(ConnectionPoolEvent evt);
	/**
	 * Called when a check-out request causes the poolSize limit to be reached.
	 */
	public void maxPoolLimitReached(ConnectionPoolEvent evt);
	/**
	 * Called when a check-out request causes the poolSize limit to be exceeded.
	 */
	public void maxPoolLimitExceeded(ConnectionPoolEvent evt);
	/**
	 * Called when a check-out request causes the maxSize limit to be reached.
	 * (maxSize is equivalent to maxConn)
	 */
	public void maxSizeLimitReached(ConnectionPoolEvent evt);
	/**
	 * Called when a check-out request attempts to exceed the maxSize limit.
	 * (maxSize is equivalent to maxConn)
	 */
	public void maxSizeLimitError(ConnectionPoolEvent evt);
	/**
	 * Called when the pool's parameters are changed.
	 */
	public void poolParametersChanged(ConnectionPoolEvent evt);
	/**
	 * Called when the pool is released (no more events are fired by the pool after this event).
	 */
	public void poolReleased(ConnectionPoolEvent evt);
}