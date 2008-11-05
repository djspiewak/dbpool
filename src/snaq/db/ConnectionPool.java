/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import snaq.util.*;
import java.sql.*;
import java.util.*;

/**
 * Implementation of a database connection pool.
 * @see snaq.db.CacheConnection
 * @see snaq.db.CachedCallableStatement
 * @see snaq.db.CachedPreparedStatement
 * @author Giles Winstanley
 */
public class ConnectionPool extends ObjectPool implements Comparable
{
	private String url, user, pass;
	private Properties props;
	private ConnectionValidator validator = new DefaultValidator();
	private PasswordDecoder decoder;
	private boolean cacheSS, cachePS, cacheCS;
	private List listeners = new ArrayList();


	/**
	 * Creates new connection pool.
	 * @param name pool name
	 * @param poolSize maximum number of pooled objects, or 0 for no limit
	 * @param maxSize maximum number of possible objects, or 0 for no limit
	 * @param expiryTime expiry time (milliseconds) for pooled object, or 0 for no expiry
	 * @param url JDBC connection URL
	 * @param username database username
	 * @param password password for the database username supplied
	 */
	public ConnectionPool(String name, int poolSize, int maxSize, long expiryTime, String url, String username, String password)
	{
		super(name, poolSize, maxSize, expiryTime);
		this.url = url;
		this.user = username;
		this.pass = password;
		this.props = null;
		setCaching(true);
		addObjectPoolListener(new EventRelay());
	}

	/**
	 * Creates new connection pool.
	 * @param name pool name
	 * @param poolSize maximum number of pooled objects, or 0 for no limit
	 * @param maxSize maximum number of possible objects, or 0 for no limit
	 * @param expiryTime expiry time (milliseconds) for pooled object, or 0 for no expiry
	 * @param url JDBC connection URL
	 * @param props connection properties
	 */
	public ConnectionPool(String name, int poolSize, int maxSize, long expiryTime, String url, Properties props)
	{
		this(name, poolSize, maxSize, expiryTime, url, null, null);
		this.props = props;
		this.pass = props.getProperty("password");
		addObjectPoolListener(new EventRelay());
	}


	/**
	 * Creates a new database connection.
	 */
	protected Reusable create() throws SQLException
	{
		Connection con = null;
		CacheConnection ccon = null;
		try
		{
			if (props != null)
			{
				if (decoder != null)
					props.setProperty("password", new String(decoder.decode(pass)));
				log("Getting connection (properties): " + url);
				con = DriverManager.getConnection(url, props);
			}
			else if (user != null)
			{
				try
				{
					if (decoder != null)
					{
						log("Getting connection (user/enc.pass): " + url);
						con = DriverManager.getConnection(url, user, new String(decoder.decode(pass)));
					}
					else
					{
						log("Getting connection (user/pass): " + url);
						con = DriverManager.getConnection(url, user, pass);
					}
				}
				catch (SQLException sqle)
				{
					log("Failed to connect with standard authentication...trying with just JDBC URL");
					log("Getting connection (URL only): " + url);
					con = DriverManager.getConnection(url);
				}
			}
			else
				con = DriverManager.getConnection(url);

			// Add caching wrapper to connection
			ccon = new CacheConnection(this, con);
			ccon.setCacheStatements(cacheSS);
			ccon.setCachePreparedStatements(cachePS);
			ccon.setCacheCallableStatements(cacheCS);
			log("Created a new connection");

			// Check for warnings
			SQLWarning warn = con.getWarnings();
			while (warn != null)
			{
				log("Warning - " + warn.getMessage());
				warn = warn.getNextWarning();
			}
		}
		catch (SQLException sqle)
		{
			log(sqle, "Can't create a new connection for " + url);
			// Clean up open connection.
			try { con.close(); }
			catch (SQLException sqle2) {}
			// Rethrow exception.
			throw sqle;
		}
		return ccon;
	}

	/**
	 * Validates a connection.
	 */
	protected boolean isValid(final Reusable o)
	{
		if (o == null)
			return false;
		if (validator == null)
			return true;

		try
		{
			boolean valid = validator.isValid((Connection)o);
			if (!valid)
				fireValidationErrorEvent();
			return valid;
		}
		catch (Exception e) { log(e, "Exception during validation"); return false; }
	}

	/**
	 * Sets the validator class for connections.
	 */
	public void setValidator(ConnectionValidator cv) { validator = cv; }

	/**
	 * Returns the current validator class.
	 */
	public ConnectionValidator getValidator() { return validator; }

	/**
	 * Sets the password decoder class.
	 */
	public void setPasswordDecoder(PasswordDecoder pd) { decoder = pd; }

	/**
	 * Returns the current password decoder class.
	 */
	public PasswordDecoder getPasswordDecoder() { return decoder; }

	/**
	 * Closes the given connection.
	 */
	protected void destroy(final Reusable o)
	{
		if (o == null)
			return;
		try
		{
			((CacheConnection)o).release();
			log("Destroyed connection");
		}
		catch (SQLException e)
		{
			log(e, "Can't destroy connection");
		}
	}

	/**
	 * Gets a connection from the pool.
	 */
	public Connection getConnection() throws SQLException
	{
		try
		{
			Reusable o = super.checkOut();
			if (o != null)
			{
				CacheConnection cc = (CacheConnection)o;
				cc.setOpen();
				return cc;
			}
			return null;
		}
		catch (Exception e)
		{
			log(e, "Error getting connection");
			if (e instanceof SQLException)
				throw (SQLException)e;
			else
			{
				Throwable t = e.getCause();
				while (t != null)
				{
					log(e, "Error getting connection");
					t = t.getCause();
				}
				throw new SQLException(e.getMessage());
			}
		}
	}

	/**
	 * Gets a connection from the pool.
	 */
	public Connection getConnection(long timeout) throws SQLException
	{
		try
		{
			Object o = super.checkOut(timeout);
			if (o != null)
			{
				CacheConnection cc = (CacheConnection)o;
				cc.setOpen();
				return cc;
			}
			return null;
		}
		catch (Exception e)
		{
			if (e instanceof SQLException)
				throw (SQLException)e;
			else
			{
				log(e, "Error getting connection");
				throw new SQLException(e.getMessage());
			}
		}
	}

	/**
	 * Returns a connection to the pool (for internal use only).
	 * Connections obtained from the pool should be returned by calling the
	 * close() method on the connection.
	 */
	protected void freeConnection(Connection c) throws SQLException
	{
		if (c == null  ||  !CacheConnection.class.isInstance(c))
			log("Attempt to return invalid item");
		else
		{
			CacheConnection cc = (CacheConnection)c;
			super.checkIn((Reusable)c);
		}
	}

	/**
	 * Determines whether to perform statement caching.
	 * This applies to all types of statements (normal, prepared, callable).
	 */
	public void setCaching(boolean b)
	{
		cacheSS = cachePS = cacheCS = b;
	}

	/**
	 * Determines whether to perform statement caching.
	 * @param ss whether to cache Statement objects
	 * @param ps whether to cache PreparedStatement objects
	 * @param cs whether to cache CallableStatement objects
	 */
	public void setCaching(boolean ss, boolean ps, boolean cs)
	{
		cacheSS = ss;
		cachePS = ps;
		cacheCS = cs;
	}

	/** Returns a descriptive string for this pool instance. */
	public String toString() { return getName(); }
	/** Compares this instances to other instances by name. */
	public int compareTo(Object o) { return this.toString().compareTo(((ConnectionPool)o).toString()); }

	//**************************
	//  Event-handling methods
	//**************************

	/**
	 * Adds an ConnectionPoolListener to the event notification list.
	 */
	public final void addConnectionPoolListener(ConnectionPoolListener x)
	{
		listeners.add(x);
	}

	/**
	 * Removes an ConnectionPoolListener from the event notification list.
	 */
	public final void removeConnectionPoolListener(ConnectionPoolListener x)
	{
		listeners.remove(x);
	}

	private final void fireValidationErrorEvent()
	{
		if (listeners.isEmpty())
			return;
		ConnectionPoolEvent poolEvent = new ConnectionPoolEvent(this, ConnectionPoolEvent.VALIDATION_ERROR);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ConnectionPoolListener)iter.next()).validationError(poolEvent);
	}

	private final void firePoolCheckOutEvent()
	{
		if (listeners.isEmpty())
			return;
		ConnectionPoolEvent poolEvent = new ConnectionPoolEvent(this, ConnectionPoolEvent.CHECKOUT);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ConnectionPoolListener)iter.next()).poolCheckOut(poolEvent);
	}

	private final void firePoolCheckInEvent()
	{
		if (listeners.isEmpty())
			return;
		ConnectionPoolEvent poolEvent = new ConnectionPoolEvent(this, ConnectionPoolEvent.CHECKIN);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ConnectionPoolListener)iter.next()).poolCheckIn(poolEvent);
	}

	private final void fireMaxPoolLimitReachedEvent()
	{
		if (listeners.isEmpty())
			return;
		ConnectionPoolEvent poolEvent = new ConnectionPoolEvent(this, ConnectionPoolEvent.MAX_POOL_LIMIT_REACHED);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ConnectionPoolListener)iter.next()).maxPoolLimitReached(poolEvent);
	}

	private final void fireMaxPoolLimitExceededEvent()
	{
		if (listeners.isEmpty())
			return;
		ConnectionPoolEvent poolEvent = new ConnectionPoolEvent(this, ConnectionPoolEvent.MAX_POOL_LIMIT_EXCEEDED);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ConnectionPoolListener)iter.next()).maxPoolLimitExceeded(poolEvent);
	}

	private final void fireMaxSizeLimitReachedEvent()
	{
		if (listeners.isEmpty())
			return;
		ConnectionPoolEvent poolEvent = new ConnectionPoolEvent(this, ConnectionPoolEvent.MAX_SIZE_LIMIT_REACHED);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ConnectionPoolListener)iter.next()).maxSizeLimitReached(poolEvent);
	}

	private final void fireMaxSizeLimitErrorEvent()
	{
		if (listeners.isEmpty())
			return;
		ConnectionPoolEvent poolEvent = new ConnectionPoolEvent(this, ConnectionPoolEvent.MAX_SIZE_LIMIT_ERROR);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ConnectionPoolListener)iter.next()).maxSizeLimitError(poolEvent);
	}

	private final void fireParametersChangedEvent()
	{
		if (listeners == null  ||  listeners.isEmpty())
			return;
		ConnectionPoolEvent poolEvent = new ConnectionPoolEvent(this, ConnectionPoolEvent.PARAMETERS_CHANGED);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ConnectionPoolListener)iter.next()).poolParametersChanged(poolEvent);
	}

	private final void firePoolReleasedEvent()
	{
		if (listeners.isEmpty())
			return;
		ConnectionPoolEvent poolEvent = new ConnectionPoolEvent(this, ConnectionPoolEvent.POOL_RELEASED);
		for (Iterator iter = listeners.iterator(); iter.hasNext();)
			((ConnectionPoolListener)iter.next()).poolReleased(poolEvent);
	}


	/**
	 * Class to relay ObjectPoolEvents as ConnectionPoolEvents for convenience.
	 */
	private final class EventRelay extends ObjectPoolEventAdapter
	{
		public void poolCheckOut(ObjectPoolEvent evt) { firePoolCheckOutEvent(); }
		public void poolCheckIn(ObjectPoolEvent evt) { firePoolCheckInEvent(); }
		public void maxPoolLimitReached(ObjectPoolEvent evt) { fireMaxPoolLimitReachedEvent(); }
		public void maxPoolLimitExceeded(ObjectPoolEvent evt) { fireMaxPoolLimitExceededEvent(); }
		public void maxSizeLimitReached(ObjectPoolEvent evt) { fireMaxSizeLimitReachedEvent(); }
		public void maxSizeLimitError(ObjectPoolEvent evt) { fireMaxSizeLimitErrorEvent(); }
		public void poolParametersChanged(ObjectPoolEvent evt) { fireParametersChangedEvent(); }
		public void poolReleased(ObjectPoolEvent evt) { firePoolReleasedEvent(); listeners.clear(); }
	}


	/**
	 * Default implementation of ConnectionValidator.
	 * This class simply checks a Connection with the <tt>isClosed()</tt> method.
	 */
	static class DefaultValidator implements ConnectionValidator
	{
		/**
		 * Validates a connection.
		 */
		public boolean isValid(Connection con)
		{
			try { return !con.isClosed(); }
			catch (SQLException e) { return false; }
		}
	}
}