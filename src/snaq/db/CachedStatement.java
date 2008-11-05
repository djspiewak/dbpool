/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import snaq.util.Reusable;
import java.sql.*;

/**
 * Statement wrapper that provides methods for caching support.
 * @author Giles Winstanley
 */
public class CachedStatement implements Statement, Reusable
{
	protected StatementListener listener;
	protected Statement st;
	protected boolean open = true;  // Exists to avoid repeat cleaning of statement


	/**
	 * Creates a new CachedStatement object, using the supplied Statement.
	 */
	public CachedStatement(Statement st)
	{
		this.st = st;
	}

	/** Returns a string descriptions of the ResultSet parameters. */
	protected String getParametersString()
	{
		StringBuffer sb = new StringBuffer();
		try
		{
			switch(getResultSetType())
			{
				case ResultSet.TYPE_SCROLL_INSENSITIVE:
					sb.append("TYPE_SCROLL_INSENSITIVE");
					break;
				case ResultSet.TYPE_SCROLL_SENSITIVE:
					sb.append("TYPE_SCROLL_SENSITIVE");
					break;
				default:
					sb.append("TYPE_FORWARD_ONLY");
			}
		}
		catch (SQLException sqle) { sb.append("TYPE_UNKNOWN"); }
		sb.append(',');
		try
		{
			switch(getResultSetConcurrency())
			{
				case ResultSet.CONCUR_UPDATABLE:
					sb.append("CONCUR_UPDATABLE");
					break;
				default:
					sb.append("CONCUR_READ_ONLY");
			}
		}
		catch (SQLException sqle) { sb.append("CONCUR_UNKNOWN"); }
		sb.append(',');
		try
		{
			switch(getResultSetHoldability())
			{
				case ResultSet.CLOSE_CURSORS_AT_COMMIT:
					sb.append("CLOSE_CURSORS_AT_COMMIT");
					break;
				case ResultSet.HOLD_CURSORS_OVER_COMMIT:
					sb.append("HOLD_CURSORS_OVER_COMMIT");
			}
		}
		catch (SQLException sqle) { sb.append("HOLD_UNKNOWN"); }
		return sb.toString();
	}

	// Cleans up the statement ready to be reused or closed.
	public void recycle() throws SQLException
	{
		ResultSet rs = st.getResultSet();
		if (rs != null)
			rs.close();

		try { st.clearWarnings(); }
		catch (SQLException sqle) {}    // Caught to fix bug in some drivers

		try { st.clearBatch(); }
		catch (SQLException sqle) {}    // Caught to fix bug in some drivers
	}

	/**
	 * Overridden to provide caching support.
	 */
	public void close() throws SQLException
	{
		if (!open)
			return;
		open = false;
		// If listener registered, do callback, otherwise release statement
		if (listener != null)
			listener.statementClosed(this);
		else
			release();
	}

	/**
	 * Overridden to provide caching support.
	 */
	public void release() throws SQLException
	{
		st.close();
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
	void setStatementListener(StatementListener x)
	{
		this.listener = x;
	}

	//*****************************
	// Statement interface methods
	//*****************************

	public ResultSet executeQuery(String sql) throws SQLException
	{
		return st.executeQuery(sql);
	}

	public int executeUpdate(String sql) throws SQLException
	{
		return st.executeUpdate(sql);
	}

	public int getMaxFieldSize() throws SQLException
	{
		return st.getMaxFieldSize();
	}

	public void setMaxFieldSize(int max) throws SQLException
	{
		st.setMaxFieldSize(max);
	}

	public int getMaxRows() throws SQLException
	{
		return st.getMaxRows();
	}

	public void setMaxRows(int max) throws SQLException
	{
		st.setMaxRows(max);
	}

	public void setEscapeProcessing(boolean enable) throws SQLException
	{
		st.setEscapeProcessing(enable);
	}

	public int getQueryTimeout() throws SQLException
	{
		return st.getQueryTimeout();
	}

	public void setQueryTimeout(int seconds) throws SQLException
	{
		st.setQueryTimeout(seconds);
	}

	public void cancel() throws SQLException
	{
		st.cancel();
	}

	public SQLWarning getWarnings() throws SQLException
	{
		return st.getWarnings();
	}

	public void clearWarnings() throws SQLException
	{
		st.clearWarnings();
	}

	public void setCursorName(String name) throws SQLException
	{
		st.setCursorName(name);
	}

	public boolean execute(String sql) throws SQLException
	{
		return st.execute(sql);
	}

	public ResultSet getResultSet() throws SQLException
	{
		return st.getResultSet();
	}

	public int getUpdateCount() throws SQLException
	{
		return st.getUpdateCount();
	}

	public boolean getMoreResults() throws SQLException
	{
		return st.getMoreResults();
	}

	public void setFetchDirection(int direction) throws SQLException
	{
		st.setFetchDirection(direction);
	}

	public int getFetchDirection() throws SQLException
	{
		return st.getFetchDirection();
	}

	public void setFetchSize(int rows) throws SQLException
	{
		st.setFetchSize(rows);
	}

	public int getFetchSize() throws SQLException
	{
		return st.getFetchSize();
	}

	public int getResultSetConcurrency() throws SQLException
	{
		return st.getResultSetConcurrency();
	}

	public int getResultSetType() throws SQLException
	{
		return st.getResultSetType();
	}

	public void addBatch(String sql) throws SQLException
	{
		st.addBatch(sql);
	}

	public void clearBatch() throws SQLException
	{
		st.clearBatch();
	}

	public int[] executeBatch() throws SQLException
	{
		return st.executeBatch();
	}

	public Connection getConnection() throws SQLException
	{
		return st.getConnection();
	}

	//**********************************
	// Interface methods from JDBC 3.0
	//**********************************

	public boolean getMoreResults(int current) throws SQLException
	{
		return st.getMoreResults(current);
	}

	public ResultSet getGeneratedKeys() throws SQLException
	{
		return st.getGeneratedKeys();
	}

	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException
	{
		return st.executeUpdate(sql, autoGeneratedKeys);
	}

	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException
	{
		return st.executeUpdate(sql, columnIndexes);
	}

	public int executeUpdate(String sql, String[] columnNames) throws SQLException
	{
		return st.executeUpdate(sql, columnNames);
	}

	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException
	{
		return st.execute(sql, autoGeneratedKeys);
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException
	{
		return st.execute(sql, columnIndexes);
	}

	public boolean execute(String sql, String[] columnNames) throws SQLException
	{
		return st.execute(sql, columnNames);
	}

	public int getResultSetHoldability() throws SQLException
	{
		return st.getResultSetHoldability();
	}
}