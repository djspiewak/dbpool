/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import java.sql.*;

/**
 * Interface for decoding database passwords.
 * This interface can be implemented by a class in order to provide custom
 * database password decoding. To use the custom decoder class make
 * sure you call the <tt>setPasswordDecoder</tt> method in either the
 * <tt>ConnectionPool</tt> object or the <tt>ConnectionPoolManager</tt>
 * in your code (or use the properties file version with the pool manager).
 * @author Giles Winstanley
 */
public interface PasswordDecoder
{
	char[] decode(String encoded);
}