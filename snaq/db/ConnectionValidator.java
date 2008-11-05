/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import java.sql.*;

/**
 * Interface for validating database connections.
 * This interface can be implemented by a class in order to provide custom
 * database connection validation. To use the custom validator class make
 * sure you call the <tt>setValidator</tt> method in either the
 * <tt>ConnectionPool</tt> object or the <tt>ConnectionPoolManager</tt>
 * in your code (or use the properties file version with the pool manager).
 * @author Giles Winstanley
 */
public interface ConnectionValidator
{
	boolean isValid(Connection con);
}