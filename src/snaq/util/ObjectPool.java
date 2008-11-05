/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.util;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Base class for a pool system implementation.
 * This class provides all the base functionality required and can be easily
 * extended to provide pooling support for many different types of object.
 * <p>New objects are retrieved on demand according to specified limits,
 * and the pool can also ensure an object's validity before returning it.
 * The limits which can be set for a pool include the number of items to be
 * held in the pool, and the maximum number to ever be created.
 * <p>The pool will try to maintain <tt>poolSize</tt> items open and ready
 * for use (unless that number has not yet been created), but if expiry is
 * enabled this number will reduce if the items are not used frequently.
 * If <tt>maxSize</tt> is specified then the pool will never create more
 * than this number of items, and another can only be obtained from the pool
 * if it is handed back by another client.
 * <p><tt>ObjectPool</tt> should be sub-classed to override
 * the following methods:</p>
 * <pre>
 *   protected Reusable create() throws Exception
 *   protected boolean isValid(final Reusable o)
 *   protected void destroy(final Reusable o)
 * </pre>
 * <p>It is recommended that the sub-class implements methods for obtaining
 * and returning items within the pool, casting the objects returned by this
 * class as required to the appropriate class type.
 * <p>ObjectPool also support asynchronous destruction of items, which can be
 * useful in circumstances where destruction of items held can take a long
 * time which would delay the <tt>checkIn</tt> method. This also applies
 * to the release of the pool after it's final use, which should always be
 * done using either <tt>release</tt> or <tt>releaseAsync</tt>.
 * @author Giles Winstanley
 */
public abstract class ObjectPool extends LogUtil
{
	// Pool access method
	private static final int ACCESS_FIFO = 1;
	private static final int ACCESS_LIFO = 2;
	private static final int ACCESS_RANDOM = 3;
	private int accessMethod = ACCESS_LIFO;
	// Other variables
	private String name;
	private List free, used;
	private int poolSize, maxSize;
	private long expiryTime;
	private long requests, hits;
	private boolean released = false;
	private boolean asyncDestroy = false;
	private DateFormat dateFormat;
	private Cleaner cleaner;
	private InitThread initer;
	private static int cleanerCount = 0;
	private List listeners = new ArrayList();


	/**
	 * Creates new object pool.
	 * @param name pool name
	 * @param poolSize maximum number of pooled objects, or 0 for no limit
	 * @param maxSize maximum number of possible objects, or 0 for no limit
	 * @param expiryTime expiry time (milliseconds) for pooled object, or 0 for no expiry
	 */
	protected ObjectPool(String name, int poolSize, int maxSize, long expiryTime)
	{
		Class type = getPoolClass();
		if (!List.class.isAssignableFrom(type))
			throw new RuntimeException("Invalid pool class type specified: " + type.getName() + " (must implement java.util.List)");
		try
		{
			free = (List)type.newInstance();
			used = (List)type.newInstance();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Unable to instantiate pool class type: " + type.getName());
		}
		this.name = name;
		this.setParameters(poolSize, maxSize, expiryTime);
	}

	/**
	 * Creates new object pool.
	 * @param name pool name
	 * @param poolSize maximum number of pooled objects, or 0 for no limit
	 * @param maxSize maximum number of possible objects, or 0 for no limit
	 * @param expiryTime expiry time (milliseconds) for pooled object, or 0 for no expiry
	 */
	protected ObjectPool(String name, int poolSize, int maxSize, int expiryTime)
	{
		this(name, poolSize, maxSize, (long)expiryTime);
	}


	/**
	 * Initializes the given number of items in the pool.
	 * This spawns a new thread to create them in the background.
	 */
	public final void init(int num)
	{
		if (num == 0)
			return;
		if (num > 0  &&  num <= getMaxSize())
		{
			if (initer != null)
			{
				initer.halt();
				try { initer.join(); } catch (InterruptedException ie) {}
			}
			initer = new InitThread(num);
			initer.start();
		}
		else
			throw new IllegalArgumentException("Invalid number of items specified for initialization");
	}

	/**
	 * Checks out an item from the pool. If no free item
	 * is available, a new item is created unless the maximum
	 * number of items has been reached. If a free item
	 * is not valid it is removed from the pool and another
	 * is retrieved.
	 * @return item from the pool, or null if nothing available
	 * @exception Exception if there is an error creating a new object
	 */
	protected final synchronized Reusable checkOut() throws Exception
	{
		if (released)
			throw new RuntimeException("Pool no longer valid for use");
		int oldTotalConns = used.size() + free.size();

		TimeWrapper tw = null;
		Reusable o = null;
		if (free.size() > 0)
		{
			//  Get an object from the free list
			switch(accessMethod)
			{
				case ACCESS_FIFO:
					tw = (TimeWrapper)free.remove(0);
					break;
				case ACCESS_RANDOM:
					tw = (TimeWrapper)free.remove((int)(free.size() * Math.random()));
					break;
				case ACCESS_LIFO:
				default:
					tw = (TimeWrapper)free.remove(free.size() - 1);
			}
			o = (Reusable)tw.getObject();
			boolean valid = isValid(o);
			while (!valid  &&  free.size() > 0)
			{
				destroyObject(o);
				log("Removed invalid item from pool");
				tw = (TimeWrapper)free.remove(0);
				o = (Reusable)tw.getObject();
				valid = isValid(o);
			}
			if (free.size() == 0  &&  !valid)
				o = null;
		}
		boolean hit = (o != null);

		// If no free items and can create more...create new item
		if (o == null)
		{
			if (maxSize > 0  &&  used.size() == maxSize)
				fireMaxSizeLimitErrorEvent();
			else if (used.size() < maxSize  ||  maxSize == 0)
			{
				o = create();
				if (!isValid(o))
					throw new RuntimeException("Unable to create a valid connection");
			}
		}

		// If a connection has been obtained/created, add it to used items collection
		if (o != null)
		{
			used.add(o);
			requests++;
			if (hit)
				hits++;
			firePoolCheckOutEvent();
			// Check for limit reaching so events can be fired.
			// (Events only fired on increase of connection numbers).
			int totalConns = used.size() + free.size();
			if (totalConns == poolSize  &&  totalConns > oldTotalConns)
				fireMaxPoolLimitReachedEvent();
			else if (totalConns == poolSize + 1  &&  totalConns > oldTotalConns)
				fireMaxPoolLimitExceededEvent();
			if (totalConns == maxSize  &&  totalConns > oldTotalConns)
				fireMaxSizeLimitReachedEvent();
		}
		if (debug)
		{
			String ratio = used.size() + "/" + (used.size() + free.size());
			String hitRate = " (HitRate=" + getHitRate() + "%)";
			log("Checkout - " + ratio + hitRate + (o == null ? " - null returned" : ""));
		}
		return o;
	}


	/**
	 * Checks out an item from the pool.
	 * If there is no pooled item available and the maximum number
	 * possible has not been reached, another is created.
	 * If a free item is detected as being invalid it is removed
	 * from the pool and the another is retrieved.
	 * If an item is not available and the maximum number possible
	 * has been reached, the method waits for the timeout period
	 * for one to become available by being checked in.
	 * @param timeout timeout value in milliseconds
	 * @return item from the pool, or null if nothing available within timeout period
	 * @exception Exception if there is an error creating a new object
	 */
	protected final synchronized Reusable checkOut(long timeout) throws Exception
	{
		long time = System.currentTimeMillis();
		Reusable o = null;
		o = checkOut();
		while (o == null  &&  (System.currentTimeMillis() - time < timeout))
		{
			try
			{
				if (debug)
					log("No pooled items spare...waiting for up to " + timeout + "ms");
				wait(timeout);
				o = checkOut();
			}
			catch (InterruptedException e) { log(e, "Connection checkout interrupted"); }
		}
		return o;
	}


	/**
	 * Checks an object into the pool, and notify other
	 * threads that may be waiting for one.
	 * @param o object to check in
	 */
	protected final void checkIn(Reusable o)
	{
		if (o == null)
		{
			log("Attempt to return null item");
			return;
		}

		synchronized(this)
		{
			firePoolCheckInEvent();

			// Check if item is from this pool
			if (!used.remove(o))
			{
				log("Attempt to return item not belonging to pool");
				throw new RuntimeException("Attempt to return item not belonging to pool " + name);
			}

			// If pool is full destroy object, else place in pool
			boolean kill = maxSize > 0  &&  (free.size() + used.size() >= poolSize);
			kill = kill  ||  (maxSize == 0  &&  free.size() >= poolSize);
			if (kill)
			{
				destroyObject(o);
				if (debug)
					log("Checkin* - " + used.size() + "/" + (used.size()+free.size()));
			}
			else
			{
				try
				{
					//  Recycle object for next use
					o.recycle();
					//  Add object to free list
					free.add(new TimeWrapper(null, o, expiryTime));
					if (debug)
						log("Checkin  - " + used.size() + "/" + (used.size()+free.size()));
					notifyAll();
				}
				catch (Exception e)
				{
					// If unable to recycle object, destroy it
					destroyObject(o);
					log(e, "Unable to recycle item - destroyed");
				}
			}
		}
	}


	/**
	 * Releases all items from the pool, and shuts the pool down.
	 * If any items are still checked-out, this method waits until all items have
	 * been checked-in before returning.
	 */
	public final void release() { release(false); }

	/**
	 * Releases all items from the pool, and shuts the pool down.
	 * This method returns immediately; a background thread is created to perform the release.
	 */
	public final synchronized void releaseAsync() { releaseAsync(false); }

	/**
	 * Forcibly releases all items from the pool, and shuts the pool down.
	 * If any items are still checked-out, this method forcibly destroys them
	 * and then returns.
	 */
	public final void releaseForcibly() { release(true); }

	/**
	 * Releases all items from the pool, and shuts the pool down.
	 * @param forced whether to forcibly destroy items, or let them be checked-in
	 */
	private final void release(boolean forced)
	{
		// Set released flag to prevent check-out of new items
		if (released)
			return;
		released = true;
		//  Destroy cleaner thread
		if (cleaner != null)
		{
			cleaner.halt();
			try { cleaner.join(); }
			catch (InterruptedException ie) { log(ie, "Interrupted during halting of old cleaner thread"); }
			cleaner = null;
		}

		synchronized(this)
		{
			int rel = 0, failed = 0;
			TimeWrapper tw = null;
			Reusable o = null;
			// Destroy all items still in use
			if (forced)
			{
				for (Iterator iter = used.iterator(); iter.hasNext();)
				{
					o = (Reusable)iter.next();
					try
					{
						destroy(o);
						rel++;
					}
					catch (Exception ex)
					{
						failed++;
						log(ex, "Unable to release item in pool");
					}
				}
				used.clear();
			}
			else
			{
				if (debug  &&  used.size() > 0)
					log("Waiting for used items to be checked-in...");
				while (used.size() > 0)
				{
					try { wait(); }
					catch (InterruptedException e) {}
				}
			}

			// Destroy all currently free items
			for (Iterator iter = free.iterator(); iter.hasNext();)
			{
				tw = (TimeWrapper)iter.next();
				o = (Reusable)tw.getObject();
				try
				{
					destroy(o);
					rel++;
				}
				catch (Exception ex)
				{
					failed++;
					log(ex, "Unable to release item in pool");
				}
			}
			free.clear();

			//  Destroy log reference
			if (debug)
			{
				String s = "Released " + rel + (rel > 1 ? " items" : " item");
				if (failed > 0)
					s += " (failed to release " + failed + (failed > 1 ? " items)" : " item)");
				log(s);
			}
			firePoolReleasedEvent();
			listeners.clear();
			super.close();
		}
	}

	/**
	 * Releases all items from the pool, and shuts the pool down.
	 * This method returns immediately; a background thread is created to perform the release.
	 */
	private final void releaseAsync(final boolean forced)
	{
		Thread t = new Thread(new Runnable()
		{
			public void run() { release(forced); }
		});
		t.start();
	}


	/**
	 * Object creation method.
	 * This method is called when a new item needs to be created following a call
	 * to one of the check-out methods.
	 * @exception Exception if unable to create the item
	 */
	protected abstract Reusable create() throws Exception;

	/**
	 * Object validation method.
	 * This method is called when checking-out an item to see if it is valid for use.
	 * When overridden by the sub-class it is recommended that this method perform
	 * suitable checks to ensure the object can be used without problems.
	 */
	protected abstract boolean isValid(final Reusable o);

	/**
	 * Object destruction method.
	 * This method is called when an object needs to be destroyed due to pool
	 * pruning/cleaning, or final release of the pool.
	 */
	protected abstract void destroy(final Reusable o);

	/**
	 * Destroys the given object (asynchronously if necessary).
	 */
	private final void destroyObject(final Reusable o)
	{
		if (o == null)
			return;
		if (asyncDestroy)
		{
			Thread t = new Thread(new Runnable()
			{
				public void run() { destroy(o); }
			});
			t.start();
		}
		else
			destroy(o);
	}

	/**
	 * Determines whether to perform asynchronous object destruction.
	 * If set to true then each time an object is destroyed (invalid object
	 * during pool operation, or when the pool is finally released) the operation
	 * is done in a separate thread, allowing the method to return immediately.
	 * This can be useful when calling the destroy method on an object takes a
	 * long time to complete.
	 */
	public final void setAsyncDestroy(boolean b) { asyncDestroy = b; }

	/**
	 * Returns whether asynchronous object destruction is enabled.
	 * (Default: false)
	 */
	public final boolean isAsyncDestroy() { return asyncDestroy; }

	/**
	 * Writes a message to the log.
	 */
	public void log(String logEntry)
	{
		log(name + ": ", logEntry);
	}

	/**
	 * Writes a message with an Exception to the log file.
	 */
	public void log(Throwable e, String logEntry)
	{
		log(e, name + ": ", logEntry);
	}

	/**
	 * Returns the pool name.
	 */
	public final String getName() { return this.name; }

	/**
	 * Returns the maximum size of the pool.
	 */
	public final int getPoolSize() { return poolSize; }

	/**
	 * Returns the maximum number of items that can be created.
	 */
	public final int getMaxSize() { return maxSize; }

	/**
	 * Returns the expiry time for unused items in the pool.
	 */
	public final long getExpiryTime() { return expiryTime; }

	/**
	 * Sets the pooling parameters.
	 * Any items currently in the pool will remain, subject to the new parameters.
	 * (The hit rate counters are reset when this method is called.)
	 * @param poolSize number of items to be keep in pool
	 * @param maxSize maximum number of items to be created
	 * @param expiryTime expiry time for unused items (milliseconds) (0 = no expiry)
	 */
	public final void setParameters(int poolSize, int maxSize, long expiryTime)
	{
		synchronized(this)
		{
			if (cleaner != null)
				cleaner.halt();

			this.poolSize = Math.max(poolSize, 0);
			this.maxSize = Math.max(maxSize, 0);
			this.expiryTime = Math.max(expiryTime, 0);
			if (this.maxSize > 0  &&  this.maxSize < this.poolSize)
				this.maxSize = this.poolSize;
			resetHitCounter();

			//  Update pooled items to use new expiry
			TimeWrapper tw = null;
			for (Iterator iter = free.iterator(); iter.hasNext();)
			{
				tw = (TimeWrapper)iter.next();
				tw.setLiveTime(expiryTime);
			}
			//  Creates cleaner thread with check interval of at most 5 seconds.
			if (this.expiryTime > 0)
			{
				long iVal = Math.min(5000, this.expiryTime / 5);
				(cleaner = new Cleaner(this, iVal)).start();
			}
		}
		if (debug)
		{
			String info = "pool=" + this.poolSize + ",max=" + this.maxSize + ",expiry=";
			info += this.expiryTime == 0 ? "none" : this.expiryTime + "ms";
			log("Parameters changed (" + info + ")");
		}
		fireParametersChangedEvent();
	}

	/**
	 * Returns the total number of objects held (available and checked-out).
	 */
	public final synchronized int getSize() { return free.size() + used.size(); }

	/**
	 * Returns the number of items that are currently checked-out.
	 */
	public final synchronized int getCheckedOut() { return used.size(); }

	/**
	 * Returns the number of items held in the pool that are free to be checked-out.
	 */
	public final synchronized int getFreeCount() { return free.size(); }

	/**
	 * Returns hit rate of the pool as a percentage.
	 * The hit rate is the proportion of requests for a connection which result
	 * in the return of a connection which is in the pool, rather than which
	 * results in the creation of a new connection.
	 */
	public final float getHitRate() { return (requests == 0) ? 0 : (((float)hits / requests) * 100f); }

	/**
	 * Resets the counter for determining the pool's hit rate.
	 */
	protected final void resetHitCounter() { requests = hits = 0; }

	/**
	 * Sets the pool access method to FIFO (first-in, first-out: a queue).
	 */
	protected final void setAccessFIFO() { accessMethod = ACCESS_FIFO; }

	/**
	 * Sets the pool access method to LIFO (last-in, first-out: a stack).
	 */
	protected final void setAccessLIFO() { accessMethod = ACCESS_LIFO; }

	/**
	 * Sets the pool access method to random (a random connection is selected for check-out).
	 */
	protected final void setAccessRandom() { accessMethod = ACCESS_RANDOM; }

	/**
	 * Returns the class to use for the pool collection.
	 * This can be over-ridden by a sub-class to provide a different List
	 * type for the pool, which may give performance benefits in certain situations.
	 * Only instances of List collections should be used.
	 * (Default: java.util.ArrayList.class)
	 * For those wanting to override this method, pool items are checked-out from
	 * the front of the List - <tt>remove(0)</tt> - and replaced at the end of
	 * the List when checked-in again - <tt>add(Object)</tt>.
	 */
	protected Class getPoolClass() { return ArrayList.class; }

	/**
	 * Shuts down the object pool.
	 * If overridden the sub-class should make sure super.finalize() is called.
	 */
	public void finalize()
	{
		if (cleaner != null)
		{
			cleaner.halt();
			cleaner = null;
		}
		if (initer != null)
		{
			initer.halt();
			initer = null;
		}
	}

	/**
	 * Flushes the pool of all currently available items, emptying the pool.
	 */
	public final void flush()
	{
		int count = 0;
		synchronized(this)
		{
			TimeWrapper tw = null;
			for (Iterator iter = free.iterator(); iter.hasNext();)
			{
				tw = (TimeWrapper)iter.next();
				iter.remove();
				destroyObject((Reusable)tw.getObject());
				count++;
			}
		}
		if (count > 0  &&  debug)
			log("Flushed all spare items from pool");
	}

	/**
	 * Purges expired objects from the pool.
	 * This method is called by the cleaner thread to purge expired items.
	 * @return false if pool is empty after purging (no further purge required until items added), true otherwise
	 */
	final synchronized boolean purge()
	{
		if (debug)
			log("Checking for expired items");
		int count = 0;
		TimeWrapper tw = null;
		for (Iterator iter = free.iterator(); iter.hasNext();)
		{
			tw = (TimeWrapper)iter.next();
			if (tw.isExpired())
			{
				iter.remove();
				destroyObject((Reusable)tw.getObject());
				count++;
			}
		}
		return free.size() > 0  ||  count > 0;
	}

	//**************************
	//  Event-handling methods
	//**************************

	/**
	 * Adds an ObjectPoolListener to the event notification list.
	 */
	public final void addObjectPoolListener(ObjectPoolListener x)
	{
		listeners.add(x);
	}

	/**
	 * Removes an ObjectPoolListener from the event notification list.
	 */
	public final void removeObjectPoolListener(ObjectPoolListener x)
	{
		listeners.remove(x);
	}

	private final void firePoolCheckOutEvent()
	{
		if (listeners.isEmpty())
			return;
		ObjectPoolEvent poolEvent = new ObjectPoolEvent(this, ObjectPoolEvent.CHECKOUT);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ObjectPoolListener)iter.next()).poolCheckOut(poolEvent);
	}

	private final void firePoolCheckInEvent()
	{
		if (listeners.isEmpty())
			return;
		ObjectPoolEvent poolEvent = new ObjectPoolEvent(this, ObjectPoolEvent.CHECKIN);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ObjectPoolListener)iter.next()).poolCheckIn(poolEvent);
	}

	private final void fireMaxPoolLimitReachedEvent()
	{
		if (listeners.isEmpty())
			return;
		ObjectPoolEvent poolEvent = new ObjectPoolEvent(this, ObjectPoolEvent.MAX_POOL_LIMIT_REACHED);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ObjectPoolListener)iter.next()).maxPoolLimitReached(poolEvent);
	}

	private final void fireMaxPoolLimitExceededEvent()
	{
		if (listeners.isEmpty())
			return;
		ObjectPoolEvent poolEvent = new ObjectPoolEvent(this, ObjectPoolEvent.MAX_POOL_LIMIT_EXCEEDED);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ObjectPoolListener)iter.next()).maxPoolLimitExceeded(poolEvent);
	}

	private final void fireMaxSizeLimitReachedEvent()
	{
		if (listeners.isEmpty())
			return;
		ObjectPoolEvent poolEvent = new ObjectPoolEvent(this, ObjectPoolEvent.MAX_SIZE_LIMIT_REACHED);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ObjectPoolListener)iter.next()).maxSizeLimitReached(poolEvent);
	}

	private final void fireMaxSizeLimitErrorEvent()
	{
		if (listeners.isEmpty())
			return;
		ObjectPoolEvent poolEvent = new ObjectPoolEvent(this, ObjectPoolEvent.MAX_SIZE_LIMIT_ERROR);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ObjectPoolListener)iter.next()).maxSizeLimitError(poolEvent);
	}

	private final void fireParametersChangedEvent()
	{
		if (listeners == null  ||  listeners.isEmpty())
			return;
		ObjectPoolEvent poolEvent = new ObjectPoolEvent(this, ObjectPoolEvent.PARAMETERS_CHANGED);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ObjectPoolListener)iter.next()).poolParametersChanged(poolEvent);
	}

	private final void firePoolReleasedEvent()
	{
		if (listeners.isEmpty())
			return;
		ObjectPoolEvent poolEvent = new ObjectPoolEvent(this, ObjectPoolEvent.POOL_RELEASED);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ObjectPoolListener)iter.next()).poolReleased(poolEvent);
	}


	/**
	 * Thread to perform clean-up of expired objects in pool.
	 * Each time nothing is cleaned because the pool is empty the cleaner waits
	 * until an item is returned, when it is woken up and starts cleaning again.
	 */
	final class Cleaner extends Thread
	{
		private ObjectPool pool;
		private long interval;
		private boolean stopped;

		Cleaner(ObjectPool pool, long interval)
		{
			this.setName("CleanerThread_" + Integer.toString(cleanerCount++));
			this.pool = pool;
			this.interval = interval;
		}

		public void start()
		{
			stopped = false;
			super.start();
		}

		/**
		 * Safely stops the thread from running.
		 */
		public void halt()
		{
			if (!isHalted())
			{
				stopped = true;
				interrupt();  // Wake cleaner if necessary
			}
		}

		/**
		 * Returns whether the thread has been halted.
		 */
		public boolean isHalted() { return stopped; }

		/**
		 * Handles the expiry of old objects.
		 */
		public void run()
		{
			while (pool.cleaner == Thread.currentThread()  &&  !stopped)
			{
				synchronized(pool)
				{
					if (!pool.purge())
					{
						try { pool.wait(); }
						catch (InterruptedException e) {}
					}
				}
				if (!stopped)
				{
					try { sleep(interval); }
					catch (InterruptedException e) {}
				}
			}
		}
	}


	/**
	 * Thread to initialize items in pool.
	 * This thread simply performs a check-out/in of new items up to the specified
	 * number to ensure the pool is populated.
	 */
	private class InitThread extends Thread
	{
		private int num;
		private boolean stopped = false;

		private InitThread(int num)
		{
			// Ensure 0 < num < poolSize.
			this.num = Math.min(poolSize, Math.max(num, 0));
		}

		public void halt() { stopped = true; }

		/**
		 * Populates the pool with the given number of database connections.
		 * If the pool already contains open connections then they will be counted
		 * towards the number created by this method.
		 */
		public void run()
		{
			if (num > 0  &&  num <= poolSize  &&  getSize() < num)
			{
				int count = 0;
				while (!stopped  &&  getSize() < num  &&  num <= poolSize)
				{
					try
					{
						Reusable o = create();
						if (o == null)
							throw new RuntimeException("Null item created");
						else
						{
							free.add(new TimeWrapper(null, o, expiryTime));
							count++;
							if (debug)
								log("Initialized new item in pool");
						}
					}
					catch (Exception ex)
					{
						log(ex, "Unable to initialize items in pool");
						stopped = true;
					}
				}
				if (debug)
					log("Initialized pool with " + count + " new item" + (count > 1 ? "s" : ""));
			}
		}
	}
}