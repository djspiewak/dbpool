/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import java.math.*;
import java.net.*;
import java.sql.*;
import java.util.Calendar;
import java.io.InputStream;
import java.io.Reader;

/**
 * PreparedStatement wrapper that provides caching support.
 * @author Giles Winstanley
 */
public class CachedPreparedStatement extends CachedStatement implements PreparedStatement
{
	protected String sql;

	/**
	 * Creates a new CachedPreparedStatement object, using the supplied PreparedStatement.
	 */
	public CachedPreparedStatement(String sql, PreparedStatement st)
	{
		super(st);
		this.sql = sql;
	}

	String getSQLString()
	{
		return sql;
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append(" [");
		sb.append(sql);
		sb.append(',');
		sb.append(getParametersString());
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Overridden to add PreparedStatement specific code.
	 */
	public void recycle() throws SQLException
	{
		super.recycle();
		PreparedStatement ps = (PreparedStatement)st;

		try { ps.clearParameters(); }
		catch (NullPointerException npe) {}		// Catch clearParameters() bug in Java when no parameters
		catch (SQLException sqle) {}    // Caught to fix bug in some drivers
	}

	/**
	 * Overridden to provide caching support.
	 */
	public void release() throws SQLException
	{
		st.close();
	}

	//*************************************
	// PreparedStatement interface methods
	//*************************************

	public ResultSet executeQuery() throws SQLException
	{
		return ((PreparedStatement)st).executeQuery();
	}

	public int executeUpdate() throws SQLException
	{
		return ((PreparedStatement)st).executeUpdate();
	}

	public void setNull(int parameterIndex, int sqlType) throws SQLException
	{
		((PreparedStatement)st).setNull(parameterIndex, sqlType);
	}

	public void setBoolean(int parameterIndex, boolean x) throws SQLException
	{
		((PreparedStatement)st).setBoolean(parameterIndex, x);
	}

	public void setByte(int parameterIndex, byte x) throws SQLException
	{
		((PreparedStatement)st).setByte(parameterIndex, x);
	}

	public void setShort(int parameterIndex, short x) throws SQLException
	{
		((PreparedStatement)st).setShort(parameterIndex, x);
	}

	public void setInt(int parameterIndex, int x) throws SQLException
	{
		((PreparedStatement)st).setInt(parameterIndex, x);
	}

	public void setLong(int parameterIndex, long x) throws SQLException
	{
		((PreparedStatement)st).setLong(parameterIndex, x);
	}

	public void setFloat(int parameterIndex, float x) throws SQLException
	{
		((PreparedStatement)st).setFloat(parameterIndex, x);
	}

	public void setDouble(int parameterIndex, double x) throws SQLException
	{
		((PreparedStatement)st).setDouble(parameterIndex, x);
	}

	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException
	{
		((PreparedStatement)st).setBigDecimal(parameterIndex, x);
	}

	public void setString(int parameterIndex, String x) throws SQLException
	{
		((PreparedStatement)st).setString(parameterIndex, x);
	}

	public void setBytes(int parameterIndex, byte[] x) throws SQLException
	{
		((PreparedStatement)st).setBytes(parameterIndex, x);
	}

	public void setDate(int parameterIndex, Date x) throws SQLException
	{
		((PreparedStatement)st).setDate(parameterIndex, x);
	}

	public void setTime(int parameterIndex, Time x) throws SQLException
	{
		((PreparedStatement)st).setTime(parameterIndex, x);
	}

	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException
	{
		((PreparedStatement)st).setTimestamp(parameterIndex, x);
	}

	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		((PreparedStatement)st).setAsciiStream(parameterIndex, x, length);
	}

	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		((PreparedStatement)st).setUnicodeStream(parameterIndex, x, length);
	}

	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		((PreparedStatement)st).setBinaryStream(parameterIndex, x, length);
	}

	public void clearParameters() throws SQLException
	{
		((PreparedStatement)st).clearParameters();
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException
	{
		((PreparedStatement)st).setObject(parameterIndex, x, targetSqlType, scale);
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException
	{
		((PreparedStatement)st).setObject(parameterIndex, x, targetSqlType);
	}

	public void setObject(int parameterIndex, Object x) throws SQLException
	{
		((PreparedStatement)st).setObject(parameterIndex, x);
	}

	public boolean execute() throws SQLException
	{
		return ((PreparedStatement)st).execute();
	}

	public void addBatch() throws SQLException
	{
		((PreparedStatement)st).addBatch();
	}

	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException
	{
		((PreparedStatement)st).setCharacterStream(parameterIndex, reader, length);
	}

	public void setRef(int i, Ref x) throws SQLException
	{
		((PreparedStatement)st).setRef(i, x);
	}

	public void setBlob(int i, Blob x) throws SQLException
	{
		((PreparedStatement)st).setBlob(i, x);
	}

	public void setClob(int i, Clob x) throws SQLException
	{
		((PreparedStatement)st).setClob(i, x);
	}

	public void setArray(int i, Array x) throws SQLException
	{
		((PreparedStatement)st).setArray(i, x);
	}

	public ResultSetMetaData getMetaData() throws SQLException
	{
		return ((PreparedStatement)st).getMetaData();
	}

	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException
	{
		((PreparedStatement)st).setDate(parameterIndex, x, cal);
	}

	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException
	{
		((PreparedStatement)st).setTime(parameterIndex, x, cal);
	}

	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException
	{
		((PreparedStatement)st).setTimestamp(parameterIndex, x, cal);
	}

	public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException
	{
		((PreparedStatement)st).setNull(paramIndex, sqlType, typeName);
	}

	//**********************************
	// Interface methods from JDBC 3.0
	//**********************************

	public ParameterMetaData getParameterMetaData() throws SQLException
	{
		return ((PreparedStatement)st).getParameterMetaData();
	}

	public void setURL(int parameterIndex, URL x) throws SQLException
	{
		((PreparedStatement)st).setURL(parameterIndex, x);
	}
}