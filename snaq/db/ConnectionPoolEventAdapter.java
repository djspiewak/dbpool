/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import snaq.util.ObjectPoolEventAdapter;

/**
 * Adapter implementation for handling PoolEvents for an ObjectPool.
 * Provides null implementations of all listener methods so a sub-class can
 * simply override the ones required.
 * @author Giles Winstanley
 */
public class ConnectionPoolEventAdapter extends ObjectPoolEventAdapter implements ConnectionPoolListener
{
	public void validationError(ConnectionPoolEvent evt) {}
	public void poolCheckOut(ConnectionPoolEvent evt) {}
	public void poolCheckIn(ConnectionPoolEvent evt) {}
	public void maxPoolLimitReached(ConnectionPoolEvent evt) {}
	public void maxPoolLimitExceeded(ConnectionPoolEvent evt) {}
	public void maxSizeLimitReached(ConnectionPoolEvent evt) {}
	public void maxSizeLimitError(ConnectionPoolEvent evt) {}
	public void poolParametersChanged(ConnectionPoolEvent evt) {}
	public void poolReleased(ConnectionPoolEvent evt) {}
}