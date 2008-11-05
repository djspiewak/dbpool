/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.util;

/**
 * Adapter implementation for handling PoolEvents for an ObjectPool.
 * Provides null implementations of all listener methods so a sub-class can
 * simply override the ones required.
 * @author Giles Winstanley
 */
public class ObjectPoolEventAdapter implements ObjectPoolListener
{
	public void poolCheckIn(ObjectPoolEvent evt) {}
	public void poolCheckOut(ObjectPoolEvent evt) {}
	public void maxPoolLimitReached(ObjectPoolEvent evt) {}
	public void maxPoolLimitExceeded(ObjectPoolEvent evt) {}
	public void maxSizeLimitReached(ObjectPoolEvent evt) {}
	public void maxSizeLimitError(ObjectPoolEvent evt) {}
	public void poolParametersChanged(ObjectPoolEvent evt) {}
	public void poolReleased(ObjectPoolEvent evt) {}
}