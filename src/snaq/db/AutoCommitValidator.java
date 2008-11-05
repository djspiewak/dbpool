/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import java.sql.*;

/**
 * Validates database connections by issuing a <tt>setAutoCommit(true)</tt>
 * method call on the connection. This class is provided as a convenience for
 * providing basic connection validation.
 * @author Giles Winstanley
 */
public class AutoCommitValidator implements ConnectionValidator
{
	public boolean isValid(Connection con)
	{
		try
		{
			con.setAutoCommit(true);
			return true;
		}
		catch (SQLException sql) { return false; }
	}
}