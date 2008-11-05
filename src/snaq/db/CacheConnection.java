/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import snaq.util.Reusable;
import java.sql.*;
import java.util.*;

/**
 * Connection wrapper that implements statement caching.
 * @see snaq.db.CachedStatement
 * @see snaq.db.CachedPreparedStatement
 * @see snaq.db.CachedCallableStatement
 * @author Giles Winstanley
 */
public final class CacheConnection implements Connection, StatementListener, Reusable
{
	// Constants for determining ResultSet parameters for statements.
	private static int DEFAULT_RESULTSET_TYPE = ResultSet.TYPE_FORWARD_ONLY;
	private static int DEFAULT_RESULTSET_CONCURRENCY = ResultSet.CONCUR_READ_ONLY;
	private static int DEFAULT_RESULTSET_HOLDABILITY = ResultSet.HOLD_CURSORS_OVER_COMMIT;

	protected ConnectionPool pool;
	protected Connection con;
	// Statement cache (List of Statement)
	protected List ss = new ArrayList();
	protected List ssUsed = new ArrayList();
	// PreparedStatement cache (Map of List of PreparedStatement)
	protected Map ps = new HashMap();
	protected List psUsed = new ArrayList();
	// CallableStatement cache (Map of List of CallableStatement)
	protected Map cs = new HashMap();
	protected List csUsed = new ArrayList();
	// Non-cached statements
	protected List nonCachable = new ArrayList();
	// Other variables
	private boolean cacheS, cacheP, cacheC;
	private int ssReq, ssHit;
	private int psReq, psHit;
	private int csReq, csHit;
	private boolean open = true;


	/**
	 * Creates a new CacheConnection object, using the supplied Connection.
	 */
	public CacheConnection(ConnectionPool pool, Connection con)
	{
		this.pool = pool;
		this.con = con;
		setCacheAll(true);
		ssReq = ssHit = psReq = psHit = csReq = csHit = 0;
	}

	/**
	 * Added to provide caching support.
	 */
	void setOpen()
	{
		open = true;
	}

	/**
	 * Added to provide caching support.
	 */
	boolean isOpen()
	{
		return open;
	}

	/**
	 * Sets whether to use caching for Statements.
	 */
	public void setCacheStatements(boolean cache)
	{
		// Release statements if required
		if (cacheS && !cache)
		{
			try { flushSpareStatements(); }
			catch (SQLException sqle) { pool.log(sqle); }
		}
		this.cacheS = cache;
	}

	/**
	 * Sets whether to use caching for PreparedStatements.
	 */
	public void setCachePreparedStatements(boolean cache)
	{
		// Release statements if required
		if (cacheP && !cache)
		{
			try { flushSparePreparedStatements(); }
			catch (SQLException sqle) { pool.log(sqle); }
		}
		this.cacheP = cache;
	}

	/**
	 * Sets whether to use caching for CallableStatements.
	 */
	public void setCacheCallableStatements(boolean cache)
	{
		// Release statements if required
		if (cacheC && !cache)
		{
			try { flushSpareCallableStatements(); }
			catch (SQLException sqle) { pool.log(sqle); }
		}
		this.cacheC = cache;
	}

	/**
	 * Sets whether to use caching for all types of Statement.
	 */
	public void setCacheAll(boolean cache)
	{
		setCacheStatements(cache);
		setCachePreparedStatements(cache);
		setCacheCallableStatements(cache);
	}

	/** Returns whether caching of CallableStatements is enabled. */
	public boolean isCachingAllStatements() { return cacheS && cacheP && cacheC; }

	/** Returns whether caching of CallableStatements is enabled. */
	public boolean isCachingStatements() { return cacheS; }

	/** Returns whether caching of CallableStatements is enabled. */
	public boolean isCachingPreparedStatements() { return cacheP; }

	/** Returns whether caching of CallableStatements is enabled. */
	public boolean isCachingCallableStatements() { return cacheC; }

	/**
	 * Returns the raw underlying Connection object for which this provides
	 * a wrapper. This is provided as a convenience method for using database-specific
	 * features for which the Connection object needs to be upcast.
	 * (e.g. to use Oracle-specific features needs to be cast to oracle.jdbc.OracleConnection).
	 * <em>To maintain the stability of the pooling system it is important that the
	 * raw connection is not destabilized when used in this way.</em>
	 */
	public Connection getRawConnection()
	{
		return con;
	}

	//******************************
	// Connection interface methods
	//******************************

	/** Overrides method to provide caching support. */
	public Statement createStatement() throws SQLException
	{
		return createStatement(DEFAULT_RESULTSET_TYPE, DEFAULT_RESULTSET_CONCURRENCY, DEFAULT_RESULTSET_HOLDABILITY);
	}

	/** Overrides method to provide caching support. */
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
	{
		return createStatement(resultSetType, resultSetConcurrency, DEFAULT_RESULTSET_HOLDABILITY);
	}

	/** Overrides method to provide caching support. */
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		CachedStatement cs = null;
		if (!cacheS)
		{
			cs = new CachedStatement(con.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
			cs.setStatementListener(this);
			cs.setOpen();
		}
		else
		{
			synchronized(ss)
			{
				ssReq++;
				// Find Statement matching criteria required
				for (Iterator it = ss.iterator(); it.hasNext();)
				{
					CachedStatement x = (CachedStatement)it.next();
					if (x.getResultSetType() == resultSetType &&
									x.getResultSetConcurrency() == resultSetConcurrency &&
									x.getResultSetHoldability() == resultSetHoldability)
					{
						cs = x;
						it.remove();
					}
				}
				// Prepare Statement for user
				if (cs != null)
				{
					cs.setOpen();
					ssHit++;
					if (pool.isDebug())
						pool.log("Statement cache hit [" + cs.getParametersString() + "] - " + calcHitRate(ssHit, ssReq));
				}
				else
				{
					cs = new CachedStatement(con.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
					cs.setStatementListener(this);
					cs.setOpen();
					if (pool.isDebug())
						pool.log("Statement cache miss [" + cs.getParametersString() + "] - " + calcHitRate(ssHit, ssReq));
				}
			}
		}
		ssUsed.add(cs);
		return cs;
	}

	/** Overrides method to provide caching support. */
	public PreparedStatement prepareStatement(String sql) throws SQLException
	{
		return prepareStatement(sql, DEFAULT_RESULTSET_TYPE, DEFAULT_RESULTSET_CONCURRENCY, DEFAULT_RESULTSET_HOLDABILITY);
	}

	/** Overrides method to provide caching support. */
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
	{
		return prepareStatement(sql, resultSetType, resultSetConcurrency, DEFAULT_RESULTSET_HOLDABILITY);
	}

	/**
	 * Overrides method to provide caching support.
	 */
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		CachedPreparedStatement cps = null;
		if (!cacheP)
		{
			cps = new CachedPreparedStatement(sql, con.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
			cps.setStatementListener(this);
			cps.setOpen();
		}
		else
		{
			synchronized(ps)
			{
				psReq++;
				// Get List of cached PreparedStatements with matching SQL
				List list = (List)ps.get(sql);
				if (list != null && !list.isEmpty())
				{
					// Find first free PreparedStatement with matching parameters
					for (Iterator it = list.iterator(); it.hasNext();)
					{
						CachedPreparedStatement x = (CachedPreparedStatement)it.next();
						if (x.getResultSetType() == resultSetType &&
										x.getResultSetConcurrency() == resultSetConcurrency &&
										x.getResultSetHoldability() == resultSetHoldability)
						{
							cps = x;
							it.remove();
						}
					}
					// Remove cache mapping if list empty
					if (list.isEmpty())
						ps.remove(sql);
				}
				// Prepare PreparedStatement for user
				if (cps != null)
				{
					cps.setOpen();
					psHit++;
					if (pool.isDebug())
						pool.log("PreparedStatement cache hit [" + sql + "," + cps.getParametersString() + "] - " + calcHitRate(psHit, psReq));
				}
				else
				{
					cps = new CachedPreparedStatement(sql, con.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
					cps.setStatementListener(this);
					cps.setOpen();
					if (pool.isDebug())
						pool.log("PreparedStatement cache miss [" + sql + "," + cps.getParametersString() + "] - " + calcHitRate(psHit, psReq));
				}
			}
		}
		psUsed.add(cps);
		return cps;
	}

	/** Overrides method to provide caching support. */
	public CallableStatement prepareCall(String sql) throws SQLException
	{
		return prepareCall(sql, DEFAULT_RESULTSET_TYPE, DEFAULT_RESULTSET_CONCURRENCY, DEFAULT_RESULTSET_HOLDABILITY);
	}

	/** Overrides method to provide caching support. */
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
	{
		return prepareCall(sql, resultSetType, resultSetConcurrency, DEFAULT_RESULTSET_HOLDABILITY);
	}

	/**
	 * Overrides method to provide caching support.
	 */
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		CachedCallableStatement ccs = null;
		if (!cacheC)
		{
			ccs = new CachedCallableStatement(sql, con.prepareCall(sql));
			ccs.setStatementListener(this);
			ccs.setOpen();
		}
		else
		{
			synchronized(cs)
			{
				csReq++;
				// Get List of cached CallableStatements with matching SQL
				List list = (List)cs.get(sql);
				if (list != null && !list.isEmpty())
				{
					// Find first free CallableStatement with matching parameters
					for (Iterator it = list.iterator(); it.hasNext();)
					{
						CachedCallableStatement x = (CachedCallableStatement)it.next();
						if (x.getResultSetType() == resultSetType &&
										x.getResultSetConcurrency() == resultSetConcurrency &&
										x.getResultSetHoldability() == resultSetHoldability)
						{
							ccs = x;
							it.remove();
						}
					}
					// Remove cache mapping if list empty
					if (list.isEmpty())
						cs.remove(sql);
				}
				// Prepare CallableStatement for user
				if (ccs != null)
				{
					ccs.setOpen();
					csHit++;
					if (pool.isDebug())
						pool.log("CallableStatement cache hit [" + sql + "," + ccs.getParametersString() + "] - " + calcHitRate(csHit, csReq));
				}
				else
				{
					CallableStatement st = con.prepareCall(sql);
					ccs = new CachedCallableStatement(sql, st);
					ccs.setStatementListener(this);
					ccs.setOpen();
					if (pool.isDebug())
						pool.log("CallableStatement cache miss [" + sql + "," + ccs.getParametersString() + "] - " + calcHitRate(csHit, csReq));
				}
			}
		}
		csUsed.add(ccs);
		return ccs;
	}

	/**
	 * Callback invoked when a statement is closed.
	 */
	public void statementClosed(CachedStatement s) throws SQLException
	{
		if (s instanceof CachedPreparedStatement)
		{
			synchronized(ps)
			{
				String key = ((CachedPreparedStatement)s).getSQLString();
				psUsed.remove(s);
				// If caching disabled close statement
				if (!cacheP)
					s.release();
				else  // else try to recycle it
				{
					try
					{
						s.recycle();
						// Place back in cache
						List list = (List)ps.get(key);
						if (list == null)
						{
							list = new ArrayList();
							ps.put(key, list);
						}
						list.add(s);
					}
					catch (SQLException sqle)
					{
						s.release();
					}
				}
			}
		}
		else if (s instanceof CachedCallableStatement)
		{
			synchronized(cs)
			{
				String key = ((CachedCallableStatement)s).getSQLString();
				csUsed.remove(s);
				// If caching disabled close statement
				if (!cacheC)
					s.release();
				else  // else try to recycle it
				{
					try
					{
						s.recycle();
						// Place back in cache
						List list = (List)cs.get(key);
						if (list == null)
						{
							list = new ArrayList();
							cs.put(key, list);
						}
						list.add(s);
					}
					catch (SQLException sqle)
					{
						s.release();
					}
				}
			}
		}
		else if (s instanceof CachedStatement)
		{
			synchronized(ss)
			{
				ssUsed.remove(s);
				// If caching disabled close statement
				if (!cacheS)
					s.release();
				else  // else try to recycle it
				{
					try
					{
						s.recycle();
						ss.add(s);
					}
					catch (SQLException sqle)
					{
						s.release();
					}
				}
			}
		}
	}

	private String calcHitRate(int hits, int reqs)
	{
		return (reqs == 0) ? "" : (((float)hits / reqs) * 100f) + "% hit rate";
	}

	public String nativeSQL(String sql) throws SQLException
	{
		return con.nativeSQL(sql);
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException
	{
		con.setAutoCommit(autoCommit);
	}

	public boolean getAutoCommit() throws SQLException
	{
		return con.getAutoCommit();
	}

	public void commit() throws SQLException
	{
		con.commit();
	}

	public void rollback() throws SQLException
	{
		con.rollback();
	}

	/**
	 * Puts connection back in a state where it can be reused.
	 */
	public void recycle() throws SQLException
	{
		// Close all open Statements
		if (cacheS)
		{
			int count = (ssUsed != null) ? ssUsed.size() : 0;
			if (count > 0)
			{
				if (pool.isDebug())
					pool.log("Cleaning " + count + " cached Statement" + (count > 1 ? "s" : ""));
				synchronized(ssUsed)
				{
					while (!ssUsed.isEmpty())
						((Statement)ssUsed.remove(0)).close();
				}
			}
		}
		else
		{
			flushOpenStatements();
			flushSpareStatements();
		}

		// Close all open PreparedStatements
		if (cacheP)
		{
			int count = (psUsed != null) ? psUsed.size() : 0;
			if (count > 0)
			{
				if (pool.isDebug())
					pool.log("Cleaning " + count + " cached PreparedStatement" + (count > 1 ? "s" : ""));
				synchronized(psUsed)
				{
					while (!psUsed.isEmpty())
						((CachedPreparedStatement)psUsed.remove(0)).close();
				}
			}
		}
		else
		{
			flushOpenPreparedStatements();
			flushSparePreparedStatements();
		}

		// Close all open CallableStatements
		if (cacheC)
		{
			int count = (csUsed != null) ? csUsed.size() : 0;
			if (count > 0)
			{
				if (pool.isDebug())
					pool.log("Cleaning " + count + " cached CallableStatement" + (count > 1 ? "s" : ""));
				synchronized(csUsed)
				{
					while (!csUsed.isEmpty())
						((CachedCallableStatement)csUsed.remove(0)).close();
				}
			}
		}
		else
		{
			flushOpenCallableStatements();
			flushSpareCallableStatements();
		}

		// Close all open non-cachable PreparedStatements.
		flushOpenNonCachableStatements();

		// Put connection back in default state
		if (!getAutoCommit())
		{
			try { rollback(); }
			catch (SQLException sqle) { pool.log(sqle); }
			setAutoCommit(true);
		}
		clearWarnings();
		
		// Clear type map entries.
		Map tm = getTypeMap();
		if (tm != null)
			tm.clear();
	}

	/**
	 * Overrides method to provide caching support.
	 */
	public void close() throws SQLException
	{
		if (!open)
			throw new SQLException("Connection already closed");
		open = false;
		// Hand itself back to the pool
		pool.freeConnection(this);
	}

	/**
	 * Returns the current number of spare Statements that are cached.
	 */
	public int getSpareStatementCount()
	{
		return ss.size();
	}

	/**
	 * Returns the current number of Statements that are in use
	 * (not including PreparedStatements &amp; CallableStatements).
	 */
	public int getOpenStatementCount()
	{
		return ssUsed.size();
	}

	/**
	 * Returns the current number of spare PreparedStatements that are cached.
	 */
	public int getSparePreparedStatementCount()
	{
		int count = 0;
		synchronized(ps)
		{
			for (Iterator it = ps.values().iterator(); it.hasNext();)
				count += ((List)it.next()).size();
		}
		return count;
	}

	/**
	 * Returns the current number of PreparedStatements that are in use
	 * (not including CallableStatements).
	 */
	public int getOpenPreparedStatementCount()
	{
		return psUsed.size();
	}

	/**
	 * Returns the current number of spare CallableStatements that are cached.
	 */
	public int getSpareCallableStatementCount()
	{
		int count = 0;
		synchronized(cs)
		{
			for (Iterator it = cs.values().iterator(); it.hasNext();)
				count += ((List)it.next()).size();
		}
		return count;
	}

	/**
	 * Returns the current number of CallableStatements that are in use.
	 */
	public int getOpenCallableStatementCount()
	{
		return csUsed.size();
	}

	/**
	 * Returns the current number of non-cachable statements that are in use.
	 * (Currently only some PreparedStatements are non-cachable by virtue of
	 * a request made at creation for support for auto-generated keys.)
	 * @see snaq.db.CacheConnection#prepareStatement(String, int)
	 * @see snaq.db.CacheConnection#prepareStatement(String, int[])
	 * @see snaq.db.CacheConnection#prepareStatement(String, String[])
	 */
	public int getOpenNonCachableStatementCount()
	{
		return nonCachable.size();
	}

	/**
	 * Flushes the spare Statement caches for this connection.
	 */
	protected void flushSpareStatements() throws SQLException
	{
		// Close all cached Statements
		int count = (ss != null) ? ss.size() : 0;
		if (count > 0)
		{
			if (pool.isDebug())
				pool.log("Closing " + count + " cached Statement" + (count > 1 ? "s" : ""));
			synchronized(ss)
			{
				while (!ss.isEmpty())
					((CachedStatement)ss.remove(0)).release();
			}
		}
	}

	/**
	 * Flushes the open Statement cache for this connection.
	 */
	protected void flushOpenStatements() throws SQLException
	{
		// Close all open Statements
		int count = (ssUsed != null) ? ssUsed.size() : 0;
		if (count > 0)
		{
			if (pool.isDebug())
				pool.log("Closing " + count + " open Statement" + (count > 1 ? "s" : ""));
			synchronized(ssUsed)
			{
				while (!ssUsed.isEmpty())
					((CachedStatement)ssUsed.remove(0)).release();
			}
		}
	}

	/**
	 * Flushes the spare PreparedStatement cache for this connection.
	 */
	protected void flushSparePreparedStatements() throws SQLException
	{
		// Close all cached PreparedStatements
		int count = (ps != null) ? ps.size() : 0;
		if (count > 0)
		{
			if (pool.isDebug())
				pool.log("Closing " + count + " cached PreparedStatement" + (count > 1 ? "s" : ""));
			synchronized(ps)
			{
				for (Iterator iter = ps.values().iterator(); iter.hasNext();)
				{
					List list = (List)iter.next();
					for (Iterator it = list.iterator(); it.hasNext();)
						((CachedPreparedStatement)it.next()).release();
				}
				ps.clear();
			}
		}
	}

	/**
	 * Flushes the open PreparedStatement cache for this connection.
	 */
	protected void flushOpenPreparedStatements() throws SQLException
	{
		// Close all open PreparedStatements
		int count = (psUsed != null) ? psUsed.size() : 0;
		if (count > 0)
		{
			if (pool.isDebug())
				pool.log("Closing " + count + " open PreparedStatement" + (count > 1 ? "s" : ""));
			synchronized(psUsed)
			{
				while (!psUsed.isEmpty())
					((CachedPreparedStatement)psUsed.remove(0)).release();
			}
		}
	}

	/**
	 * Flushes the spare CallableStatement cache for this connection.
	 */
	protected void flushSpareCallableStatements() throws SQLException
	{
		// Close all cached CallableStatements
		int count = (cs != null) ? cs.size() : 0;
		if (count > 0)
		{
			if (pool.isDebug())
				pool.log("Closing " + count + " cached CallableStatement" + (count > 1 ? "s" : ""));
			synchronized(cs)
			{
				for (Iterator iter = cs.values().iterator(); iter.hasNext();)
				{
					List list = (List)iter.next();
					for (Iterator it = list.iterator(); it.hasNext();)
						((CachedCallableStatement)it.next()).release();
				}
				cs.clear();
			}
		}
	}

	/**
	 * Flushes the open CallableStatement cache for this connection.
	 */
	protected void flushOpenCallableStatements() throws SQLException
	{
		// Close all open CallableStatements
		int count = (csUsed != null) ? csUsed.size() : 0;
		if (count > 0)
		{
			if (pool.isDebug())
				pool.log("Closing " + count + " open CallableStatement" + (count > 1 ? "s" : ""));
			synchronized(csUsed)
			{
				while (!csUsed.isEmpty())
					((CachedCallableStatement)csUsed.remove(0)).release();
			}
		}
	}

	/**
	 * Flushes the non-cachable Statements for this connection.
	 */
	protected void flushOpenNonCachableStatements() throws SQLException
	{
		int count = (nonCachable != null) ? nonCachable.size() : 0;
		if (count > 0)
		{
			if (pool.isDebug())
				pool.log("Closing " + count + " open non-cachable Statement" + (count > 1 ? "s" : ""));
			synchronized(nonCachable)
			{
				while (!nonCachable.isEmpty())
				{
					try { ((Statement)nonCachable.remove(0)).close(); }
					catch (SQLException sqle) { pool.log(sqle); }
				}
			}
		}
	}

	/**
	 * Destroys the wrapped connection.
	 */
	public void release() throws SQLException
	{
		open = false;
		ArrayList list = new ArrayList();

		try { flushSpareStatements(); flushOpenStatements(); }
		catch (SQLException e) { list.add(e); }
		try { flushSparePreparedStatements(); flushOpenPreparedStatements(); }
		catch (SQLException e) { list.add(e); }
		try { flushSpareCallableStatements(); flushOpenCallableStatements(); }
		catch (SQLException e) { list.add(e); }
		try { flushOpenNonCachableStatements(); }
		catch (SQLException e) { list.add(e); }

		try { con.close(); }
		catch (SQLException e) { list.add(e); }

		if (!list.isEmpty())
		{
			SQLException sqle = new SQLException("Problem releasing connection resources");
			for (Iterator it = list.iterator(); it.hasNext();)
			{
				SQLException x = (SQLException)it.next();
				sqle.setNextException(x);
				sqle = x;
			}
			throw sqle;
		}
	}

	public boolean isClosed() throws SQLException
	{
		return con.isClosed();
	}

	public DatabaseMetaData getMetaData() throws SQLException
	{
		return con.getMetaData();
	}

	public void setReadOnly(boolean readOnly) throws SQLException
	{
		con.setReadOnly(readOnly);
	}

	public boolean isReadOnly() throws SQLException
	{
		return con.isReadOnly();
	}

	public void setCatalog(String catalog) throws SQLException
	{
		con.setCatalog(catalog);
	}

	public String getCatalog() throws SQLException
	{
		return con.getCatalog();
	}

	public void setTransactionIsolation(int level) throws SQLException
	{
		con.setTransactionIsolation(level);
	}

	public int getTransactionIsolation() throws SQLException
	{
		return con.getTransactionIsolation();
	}

	public SQLWarning getWarnings() throws SQLException
	{
		return con.getWarnings();
	}

	public void clearWarnings() throws SQLException
	{
		con.clearWarnings();
	}

	public Map getTypeMap() throws SQLException
	{
		return con.getTypeMap();
	}

	public void setTypeMap(Map map) throws SQLException
	{
		con.setTypeMap(map);
	}

	//**********************************
	// Interface methods from JDBC 3.0
	//**********************************

	public void setHoldability(int holdability) throws SQLException
	{
		con.setHoldability(holdability);
	}

	public int getHoldability() throws SQLException
	{
		return con.getHoldability();
	}

	public Savepoint setSavepoint() throws SQLException
	{
		return con.setSavepoint();
	}

	public Savepoint setSavepoint(String name) throws SQLException
	{
		return con.setSavepoint(name);
	}

	public void rollback(Savepoint savepoint) throws SQLException
	{
		con.rollback(savepoint);
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException
	{
		con.releaseSavepoint(savepoint);
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
	{
		PreparedStatement x = con.prepareStatement(sql, autoGeneratedKeys);
		nonCachable.add(x);
		return x;
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
	{
		PreparedStatement x = con.prepareStatement(sql, columnIndexes);
		nonCachable.add(x);
		return x;
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
	{
		PreparedStatement x = con.prepareStatement(sql, columnNames);
		nonCachable.add(x);
		return x;
	}
}