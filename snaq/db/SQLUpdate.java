/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import java.io.*;
import java.sql.*;
import java.util.StringTokenizer;

/**
 * Command-line utility to send SQL commands to a database.
 * This class is useful for easily creating a large number of database tables
 * and/or records from a user-defined file.
 * It relies on the <tt>ConnectionPoolManager</tt> class to assist
 * with the creation of a connection to the database, which in turn requires
 * the appropriate <tt>dbpool.properties</tt> file in the classpath.
 * <pre>
 * Usage: java snaq.db.SQLUpdate &lt;pool&gt; &lt;input file&gt; [&lt;separator&gt;]
 * </pre>
 * where <tt>pool</tt> is the name of the connection pool as defined in
 * the dbpool.properties file, <tt>input file</tt> is the name of the text
 * file containing the SQL statements to be issued to the defined database,
 * and <tt>separator</tt> is an optional parameter to specify a delimiter
 * for the SQL statements in the file. If the separator is not specified then
 * each line of the file is assumed to be a separate statement.
 * <p>(Note: comments are allowed in the input file by starting the line with
 * either # or --).
 * @see snaq.db.ConnectionPoolManager
 * @author Giles Winstanley
 */
public class SQLUpdate
{
	private ConnectionPoolManager cpm;
	private Connection con;
	private Statement statement;
	private String dbName;

	private ByteArrayOutputStream logBuffer;
	private PrintWriter log;


	public SQLUpdate(String db) throws IOException
	{
		dbName = db;
		cpm = ConnectionPoolManager.getInstance();
	}


	/**
	 * Opens the database connection.
	 */
	private void openConnection(String s) throws SQLException
	{
		if (s == null || s.equals(""))
		{
			System.out.println("Please specify a database name");
			System.exit(1);
		}
		con = cpm.getConnection(s);
		try { statement = con.createStatement(); }
		catch (SQLException e) {}
		logBuffer = new ByteArrayOutputStream();
		log = new PrintWriter(logBuffer);
	}


	/**
	 * Closes the database connection.
	 */
	private void closeConnection()
	{
		try
		{
			statement.close();
			con.close();
		}
		catch (SQLException e) {}

		//  Output errors to log file
		log.flush();
		log.close();
		if (logBuffer.size() > 0)
		{
			try
			{
				FileOutputStream fos = new FileOutputStream("SQLUpdate.log", true);
				fos.write(logBuffer.toByteArray());
				fos.flush();
				fos.close();
			}
			catch (IOException ioe) {}
		}
		cpm.release();
	}


	/**
	 * Issues a statement to the database.
	 */
	private void doStatement(String sql)
	{
		int count = 0;
		try
		{
			count = statement.executeUpdate(sql);
			System.out.print(".");
		}
		catch (SQLException sqle)
		{
			System.out.print("x");
			log.println();
			log.println(sql);
			log.println(sqle.getMessage());
//			sqle.printStackTrace(log);
		}
	}


	public static void main(String args[]) throws Exception
	{
		if (args == null || args.length < 2)
		{
			System.out.println("Usage: java snaq.db.SQLUpdate <database> <text file> [<separator>]");
			System.exit(0);
		}

		String db = args[0];
		String file = args[1];
		String separator = args.length < 3 ? null : args[2];

		//  Load file
		String contents = null;
		try
		{
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			FileInputStream fis = new FileInputStream(file);
			byte[] b = new byte[4096];
			int n;
			while ((n = fis.read(b)) != -1)
				bao.write(b, 0, n);
			fis.close();
			contents = new String(bao.toByteArray());
		}
		catch (IOException ioe)
		{
			System.out.println("I/O error with file " + file);
			System.exit(1);
		}

		//  Open a database connection
		SQLUpdate sql = null;
		try
		{
			sql = new SQLUpdate(db);
		}
		catch (IOException ioe)
		{
			System.err.println("Unable to create instance of SQLUpdate");
			ioe.printStackTrace();
			System.exit(1);
		}
		sql.openConnection(db);

		if (separator == null)
		{
			StringTokenizer st = new StringTokenizer(contents, "\n\r");
			while (st.hasMoreTokens())
			{
				String token = st.nextToken().trim();
				if (!token.startsWith("#")  &&  !token.equals(""))
					sql.doStatement(token);
			}
		}
		else
		{
			System.out.println("Separator: " + separator);

			StringBuffer sb = new StringBuffer();
			StringTokenizer st = new StringTokenizer(contents, "\n\r");
			while (st.hasMoreTokens())
			{
				// Get next line
				String line = st.nextToken();

				// If line is a comment...ignore it
				if (line.startsWith("#")  ||  line.startsWith("--"))
				{
					sb.setLength(0);
				}
				else
				{
					int pos = line.indexOf(separator);
					if (pos >= 0)
					{
						sb.append(line.substring(0, pos));
						sql.doStatement(sb.toString());
						sb.setLength(0);
					}
					else
						sb.append(line);
				}
			}
		}

		sql.closeConnection();
		System.out.println();
	}
}