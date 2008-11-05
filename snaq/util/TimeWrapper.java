package snaq.util;

import java.util.*;

/**
 * Wrapper for an object, useful for providing caching/pooling support.
 * @see snaq.util.CacheManager
 * @see snaq.util.ObjectPool
 * @author Giles Winstanley
 */
public class TimeWrapper implements Cacheable
{
	private Object id, obj;
	private long death = 0;
	private long accessed = System.currentTimeMillis();

	/**
	 * Creates a new wrapped object.
	 * @param id identifier object
	 * @param obj object to be referenced
	 * @param expiryTime object's idle time before death in milliseconds (0 - eternal)
	 */
	public TimeWrapper(Object id, Object obj, long expiryTime)
	{
		this.id = id;
		this.obj = obj;
		if (expiryTime > 0)
			this.death = System.currentTimeMillis() + expiryTime;
	}

	/**
	 * Returns the object's identifier.
	 */
	public Object getId()
	{
		return id;
	}

	/**
	 * Returns the object referenced by this wrapper.
	 */
	public Object getObject() { return obj; }

	/**
	 * Sets idle time allowed before this item expires.
	 * @param expiryTime idle time before expiry (0 = eternal)
	 */
	synchronized void setLiveTime(long expiryTime)
	{
		if (expiryTime < 0)
			throw new IllegalArgumentException("Invalid expiry time");
		else if (expiryTime > 0)
			this.death = System.currentTimeMillis() + expiryTime;
		else
			death = 0;
	}

	/**
	 * Whether this item has expired.
	 * (Expiry of zero indicates that it will never expire)
	 */
	public synchronized boolean isExpired()
	{
		return death > 0  &&  System.currentTimeMillis() > death;
	}

	/**
	 * Updates the time this object was last accessed.
	 */
	synchronized void updateAccessed() { accessed = System.currentTimeMillis(); }

	/**
	 * Returns the time this object was last accessed.
	 */
	long getAccessed() { return accessed; }
}