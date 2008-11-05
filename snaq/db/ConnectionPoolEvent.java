/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import snaq.util.ObjectPool;
import snaq.util.ObjectPoolEvent;

/**
 * Event for ConnectionPool objects.
 * @author Giles Winstanley
 */
public class ConnectionPoolEvent extends ObjectPoolEvent
{
	public static final int VALIDATION_ERROR = 9;


	protected ConnectionPoolEvent(ObjectPool pool, int type)
	{
		super(pool, type);
	}

	public ConnectionPool getConnectionPool() { return (ConnectionPool)getSource(); }

/*	public String getTypeString()
	{
		switch(getType())
		{
			case VALIDATION_ERROR: return "VALIDATION_ERROR";
		}
		return super.getTypeString();
	}*/

	public boolean isValidationError() { return getType() == VALIDATION_ERROR; }
}