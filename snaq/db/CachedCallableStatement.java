/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import java.io.*;
import java.math.*;
import java.net.*;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

/**
 * CallableStatement wrapper that provides caching support.
 * @author Giles Winstanley
 */
public final class CachedCallableStatement extends CachedPreparedStatement implements CallableStatement
{
	/**
	 * Creates a new CachedCallableStatement object, using the supplied CallableStatement.
	 */
	public CachedCallableStatement(String query, CallableStatement st)
	{
		super(query, st);
	}

	/**
	 * Overridden to provide caching support.
	 */
	public void release() throws SQLException
	{
		st.close();
	}

	//*************************************
	// CallableStatement interface methods
	//*************************************

	public String getString(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getString(parameterIndex);
	}

	public boolean getBoolean(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getBoolean(parameterIndex);
	}

	public byte getByte(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getByte(parameterIndex);
	}

	public short getShort(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getShort(parameterIndex);
	}

	public int getInt(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getInt(parameterIndex);
	}

	public long getLong(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getLong(parameterIndex);
	}

	public float getFloat(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getFloat(parameterIndex);
	}

	public double getDouble(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getDouble(parameterIndex);
	}

	public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException
	{
		return ((CallableStatement)st).getBigDecimal(parameterIndex, scale);
	}

	public byte[] getBytes(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getBytes(parameterIndex);
	}

	public Date getDate(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getDate(parameterIndex);
	}

	public Time getTime(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getTime(parameterIndex);
	}

	public Timestamp getTimestamp(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getTimestamp(parameterIndex);
	}

	public Object getObject(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getObject(parameterIndex);
	}

	public BigDecimal getBigDecimal(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getBigDecimal(parameterIndex);
	}

	public Object getObject(int i, Map map) throws SQLException
	{
		return ((CallableStatement)st).getObject(i, map);
	}

	public Ref getRef(int i) throws SQLException
	{
		return ((CallableStatement)st).getRef(i);
	}

	public Blob getBlob(int i) throws SQLException
	{
		return ((CallableStatement)st).getBlob(i);
	}

	public Clob getClob(int i) throws SQLException
	{
		return ((CallableStatement)st).getClob(i);
	}

	public Array getArray(int i) throws SQLException
	{
		return ((CallableStatement)st).getArray(i);
	}

	public Date getDate(int parameterIndex, Calendar cal) throws SQLException
	{
		return ((CallableStatement)st).getDate(parameterIndex, cal);
	}

	public Time getTime(int parameterIndex, Calendar cal) throws SQLException
	{
		return ((CallableStatement)st).getTime(parameterIndex, cal);
	}

	public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException
	{
		return ((CallableStatement)st).getTimestamp(parameterIndex, cal);
	}

	public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException
	{
		((CallableStatement)st).registerOutParameter(parameterIndex, sqlType);
	}

	public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException
	{
		((CallableStatement)st).registerOutParameter(parameterIndex, sqlType, scale);
	}

	public void registerOutParameter(int paramIndex, int sqlType, String typeName) throws SQLException
	{
		((CallableStatement)st).registerOutParameter(paramIndex, sqlType, typeName);
	}

	public boolean wasNull() throws SQLException
	{
		return ((CallableStatement)st).wasNull();
	}

	//**********************************
	// Interface methods from JDBC 3.0
	//**********************************

	public void registerOutParameter(String parameterName, int sqlType) throws SQLException
	{
		((CallableStatement)st).registerOutParameter(parameterName, sqlType);
	}

	public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException
	{
		((CallableStatement)st).registerOutParameter(parameterName, sqlType, scale);
	}

	public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException
	{
		((CallableStatement)st).registerOutParameter(parameterName, sqlType, typeName);
	}

	public URL getURL(int parameterIndex) throws SQLException
	{
		return ((CallableStatement)st).getURL(parameterIndex);
	}

	public void setURL(String parameterName, URL val) throws SQLException
	{
		((CallableStatement)st).setURL(parameterName, val);
	}

	public void setNull(String parameterName, int sqlType) throws SQLException
	{
		((CallableStatement)st).setNull(parameterName, sqlType);
	}

	public void setBoolean(String parameterName, boolean x) throws SQLException
	{
		((CallableStatement)st).setBoolean(parameterName, x);
	}

	public void setByte(String parameterName, byte x) throws SQLException
	{
		((CallableStatement)st).setByte(parameterName, x);
	}

	public void setShort(String parameterName, short x) throws SQLException
	{
		((CallableStatement)st).setShort(parameterName, x);
	}

	public void setDouble(String parameterName, double x) throws SQLException
	{
		((CallableStatement)st).setDouble(parameterName, x);
	}

	public void setFloat(String parameterName, float x) throws SQLException
	{
		((CallableStatement)st).setFloat(parameterName, x);
	}

	public void setInt(String parameterName, int x) throws SQLException
	{
		((CallableStatement)st).setInt(parameterName, x);
	}

	public void setLong(String parameterName, long x) throws SQLException
	{
		((CallableStatement)st).setLong(parameterName, x);
	}

	public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException
	{
		((CallableStatement)st).setBigDecimal(parameterName, x);
	}

	public void setString(String parameterName, String x) throws SQLException
	{
		((CallableStatement)st).setString(parameterName, x);
	}

	public void setBytes(String parameterName, byte[] x) throws SQLException
	{
		((CallableStatement)st).setBytes(parameterName, x);
	}

	public void setDate(String parameterName, Date x) throws SQLException
	{
		((CallableStatement)st).setDate(parameterName, x);
	}

	public void setTime(String parameterName, Time x) throws SQLException
	{
		((CallableStatement)st).setTime(parameterName, x);
	}

	public void setTimestamp(String parameterName, Timestamp x) throws SQLException
	{
		((CallableStatement)st).setTimestamp(parameterName, x);
	}

	public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException
	{
		((CallableStatement)st).setAsciiStream(parameterName, x, length);
	}

	public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException
	{
		((CallableStatement)st).setBinaryStream(parameterName, x, length);
	}

	public void setObject(String parameterName, Object x) throws SQLException
	{
		((CallableStatement)st).setObject(parameterName, x);
	}

	public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException
	{
		((CallableStatement)st).setObject(parameterName, x, targetSqlType);
	}

	public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException
	{
		((CallableStatement)st).setObject(parameterName, x, targetSqlType, scale);
	}

	public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException
	{
		((CallableStatement)st).setCharacterStream(parameterName, reader, length);
	}

	public void setDate(String parameterName, Date x, Calendar cal) throws SQLException
	{
		((CallableStatement)st).setDate(parameterName, x, cal);
	}

	public void setTime(String parameterName, Time x, Calendar cal) throws SQLException
	{
		((CallableStatement)st).setTime(parameterName, x, cal);
	}

	public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException
	{
		((CallableStatement)st).setTimestamp(parameterName, x, cal);
	}

	public void setNull(String parameterName, int sqlType, String typeName) throws SQLException
	{
		((CallableStatement)st).setNull(parameterName, sqlType, typeName);
	}

	public String getString(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getString(parameterName);
	}

	public boolean getBoolean(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getBoolean(parameterName);
	}

	public byte getByte(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getByte(parameterName);
	}

	public short getShort(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getShort(parameterName);
	}

	public int getInt(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getInt(parameterName);
	}

	public long getLong(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getLong(parameterName);
	}

	public float getFloat(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getFloat(parameterName);
	}

	public double getDouble(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getDouble(parameterName);
	}

	public byte[] getBytes(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getBytes(parameterName);
	}

	public Date getDate(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getDate(parameterName);
	}

	public Time getTime(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getTime(parameterName);
	}

	public Timestamp getTimestamp(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getTimestamp(parameterName);
	}

	public Object getObject(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getObject(parameterName);
	}

	public BigDecimal getBigDecimal(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getBigDecimal(parameterName);
	}

	public Object getObject(String parameterName, Map map) throws SQLException
	{
		return ((CallableStatement)st).getObject(parameterName, map);
	}

	public Ref getRef(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getRef(parameterName);
	}

	public Blob getBlob(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getBlob(parameterName);
	}

	public Clob getClob(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getClob(parameterName);
	}

	public Array getArray(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getArray(parameterName);
	}

	public Date getDate(String parameterName, Calendar cal) throws SQLException
	{
		return ((CallableStatement)st).getDate(parameterName, cal);
	}

	public Time getTime(String parameterName, Calendar cal) throws SQLException
	{
		return ((CallableStatement)st).getTime(parameterName, cal);
	}

	public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException
	{
		return ((CallableStatement)st).getTimestamp(parameterName, cal);
	}

	public URL getURL(String parameterName) throws SQLException
	{
		return ((CallableStatement)st).getURL(parameterName);
	}
}