/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import java.sql.*;

/**
 * Interface for a StatementListener.
 * (Implemented to provide callback support for a CacheConnection object.)
 * @author Giles Winstanley
 */
interface StatementListener
{
	/**
	 * Invoked when a Statement closes.
	 */
	void statementClosed(CachedStatement s) throws SQLException;
}
