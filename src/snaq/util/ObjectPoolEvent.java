/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.util;

import java.util.*;

/**
 * Event for ObjectPool objects.
 * @author Giles Winstanley
 */
public class ObjectPoolEvent extends EventObject
{
	public static final int CHECKOUT = 1;
	public static final int CHECKIN = 2;
	public static final int MAX_POOL_LIMIT_REACHED = 3;
	public static final int MAX_POOL_LIMIT_EXCEEDED = 4;
	public static final int MAX_SIZE_LIMIT_REACHED = 5;
	public static final int MAX_SIZE_LIMIT_ERROR = 6;
	public static final int PARAMETERS_CHANGED = 7;
	public static final int POOL_RELEASED = 8;

	private int index, type;


	/**
	 * Creates a new PoolEvent.
	 */
	public ObjectPoolEvent(ObjectPool pool, int type)
	{
		super(pool);
		this.type = type;
	}

	/**
	 * Returns the pool for which this event was created.
	 */
	public ObjectPool getPool() { return (ObjectPool)getSource(); }

	/**
	 * Returns the type of event this object represents.
	 */
	public int getType() { return type; }

	/**
	 * Returns the type of event this object represents as a string.
	 */
/*	public String getTypeString()
	{
		switch(type)
		{
			case CHECKOUT: return "CHECKOUT";
			case CHECKIN: return "CHECKIN";
			case MAX_POOL_LIMIT_REACHED: return "MAX_POOL_LIMIT_REACHED";
			case MAX_POOL_LIMIT_EXCEEDED: return "MAX_POOL_LIMIT_EXCEEDED";
			case MAX_SIZE_LIMIT_REACHED: return "MAX_SIZE_LIMIT_REACHED";
			case MAX_SIZE_LIMIT_ERROR: return "MAX_SIZE_LIMIT_ERROR";
			case PARAMETERS_CHANGED: return "PARAMETERS_CHANGED";
			case POOL_RELEASED: return "POOL_RELEASED";
		}
		return "Invalid";
	}*/

	public boolean isPoolCheckOut() { return type == CHECKOUT; }
	public boolean isPoolCheckIn() { return type == CHECKIN; }
	public boolean isMaxPoolLimitReached() { return type == MAX_POOL_LIMIT_REACHED; }
	public boolean isMaxPoolLimitExceeded() { return type == MAX_POOL_LIMIT_EXCEEDED; }
	public boolean isMaxSizeLimitReached() { return type == MAX_SIZE_LIMIT_REACHED; }
	public boolean isMaxSizeLimitError() { return type == MAX_SIZE_LIMIT_ERROR; }
	public boolean isPoolParametersChanged() { return type == PARAMETERS_CHANGED; }
	public boolean isPoolReleased() { return type == POOL_RELEASED; }
}