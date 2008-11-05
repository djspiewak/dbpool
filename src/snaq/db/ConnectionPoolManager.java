/*
	DBPool - JDBC Connection Pool Manager
	Copyright (c) Giles Winstanley
*/
package snaq.db;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import snaq.util.LogUtil;

/**
 * <p>Class to provide access and management for multiple connection pools
 * defined in a properties file or object.
 * Clients get access to each defined instance through one of the
 * static <tt>getInstance()</tt> methods and can then check-out and check-in
 * database connections from the pools defined by that manager.</p>
 *
 * <p>Each successful call to a <tt>getInstance()</tt> method also increments
 * an internal counter which keeps a record of the number of clients which hold
 * a reference to that particular pool manager. When each client has finished
 * using a pool manager it should call the <tt>release()</tt> method to
 * let the manager know that it is no longer needed by that client, so it can
 * clean up it's resources as necessary. The resources will not be released
 * until the clients counter has reached zero. <em>It is therefore necessary to
 * allocate and release your pool manager references carefully</em>.</p>
 *
 * <p>Properties for a manager can be specified in three different ways.
 * <ol>
 * <li>Properties file located in CLASSPATH
 * <li>Properties file referenced explicitly (with a File object)
 * <li>Properties object
 * </ol>
 *
 * <ol>
 * <li>A CLASSPATH located properties file can simply be accessed using the
 * method <tt>getInstance(name)</tt> where <em>name</em> is the name of the
 * properties file specified as a string.
 * <li>To specify a properties file which is not in the CLASSPATH use the
 * method <tt>getInstance(File)</tt>. This same file handle must be used
 * each time you want to obtain the instance in this way.
 * <li>To specify the pools using a Properties object a call must be made to
 * the <tt>createInstance(Properties)</tt> method. This method creates the
 * ConnectionPoolManager instance and makes it available via the <tt>getInstance()</tt>
 * method.
 * </ol>
 * <p><b>Note:</b> The <tt>getInstance()</tt> method can return one of two
 * possible instances depending on the previous calls made to the pool manager.
 * If the <tt>createInstance(Properties)</tt> method has previously been
 * called successfully then it will return this manually created instance.
 * Otherwise it will attempt to return an instance relating to the default
 * properties file (dbpool.properties) within the CLASSPATH, if it exists.</p>
 *
 * <p>The properties given to the manager specify which JDBC drivers to use to
 * access the relevant databases, and also defines the characteristics of each
 * connection pool. The properties required/allowed are as follows
 * (those marked with * are mandatory):</p>
 * <pre>
 * drivers*               Class names of required JDBC Drivers (comma/space delimited)
 * logfile*               Filename of logfile
 *
 * &lt;poolname&gt;.url*        The JDBC URL for the database
 * &lt;poolname&gt;.user        Database username for login
 * &lt;poolname&gt;.password    Database password for login
 * &lt;poolname&gt;.maxpool     The maximum number of pooled connections (0 if none)
 * &lt;poolname&gt;.maxconn     The maximum number of possible connections (0 if no limit)
 * &lt;poolname&gt;.expiry      The connection expiry time in seconds (0 if no expiry)
 * &lt;poolname&gt;.init        The initial number of connections to create (default:0)
 * &lt;poolname&gt;.validator   Class name of connection validator (optional)
 * &lt;poolname&gt;.decoder     Class name of password decoder (optional)
 * &lt;poolname&gt;.cache       Whether to cache Statements (optional, default:true)
 * &lt;poolname&gt;.debug       Whether to log debug info (optional, default:false)
 * &lt;poolname&gt;.prop.<em>XXX</em>    Passes property <em>XXX</em> and it's value to the JDBC driver
 * &lt;poolname&gt;.async       Whether to use asynchronous connection destruction (optional, default:false)
 * &lt;poolname&gt;.logfile     Filename of logfile for this pool (optional)
 * &lt;poolname&gt;.dateformat  SimpleDateFormat formatting string for log entries (optional)
 * </pre>
 *
 * <p>Multiple pools can be specified provided they each use a different pool name.
 * The <tt>validator</tt> property optionally specifies the name of a
 * class to be used for validating the database connections. The default
 * connection validation simply performs a test using <tt>Connection.isClosed()</tt>.
 * This test is not 100% reliable as the Java API documentation specifies that
 * it only returns true if the connection has been explicitly closed.
 * If more rigorous connection validation is required you can either use the
 * provided class <tt>snaq.db.AutoCommitValidator</tt> or write your own
 * validation class which should implement the <tt>ConnectionValidator</tt>
 * interface.</p>
 *
 * @see snaq.db.AutoCommitValidator
 * @see snaq.db.ConnectionValidator
 * @author Giles Winstanley
 */
public final class ConnectionPoolManager extends LogUtil implements Comparable
{
	private static final String PROPERTIES_INSTANCE_KEY = "PROPERTIES_INSTANCE";
	private static final String DEFAULT_PROPERTIES_FILE = "/dbpool.properties";
	private static Hashtable managers = new Hashtable();

	private static ArrayList drivers = new ArrayList();
	private boolean released = false;
	private HashMap pools = new HashMap();
	protected int clients;
	private Object source, key;


	private ConnectionPoolManager(Properties props, Object src)
	{
		super();
		this.source = src;
		init(props);
	}

	/** Returns a descriptive string for this instance. */
	public String toString()
	{
		if (source instanceof String)
			return "ConnectionPoolManager [CLASSPATH resource:" + source + "]";
		else if (source instanceof File)
			return "ConnectionPoolManager [File:" + ((File)source).getAbsolutePath() + "]";
		else if (source instanceof Properties)
			return "ConnectionPoolManager [Properties]";
		else
			return "ConnectionPoolManager [Unknown]";
	}

	/** Compares this instances to other instances by name. */
	public int compareTo(Object o) { return this.toString().compareTo(((ConnectionPoolManager)o).toString()); }

	/**
	 * Returns an enumeration of all the available ConnectionPoolManager instances.
	 * This method is included for convenience for external monitoring.
	 * Clients wanting to obtain an instance for using connections should NOT use
	 * this method.
	 * @deprecated Replaced by <tt>getInstances()</tt>, which returns a <tt>Set</tt>
	 */
	public static Enumeration instances() { return Collections.enumeration(getInstances()); }

	/**
	 * Returns a Set containing all the current ConnectionPoolManager instances.
	 * This method is included for convenience for external monitoring.
	 * Clients wanting to obtain an instance for using connections should NOT use
	 * this method.
	 * @return all current instances of ConnectionPoolManager.
	 */
	public static Set getInstances()
	{
		Set x = new HashSet();
		x.addAll(managers.values());
		return x;
	}

	/**
	 * Returns the singleton instance of the ConnectionPoolManager for the specified properties file.
	 * @param propsFile filename of the properties file to use (path info should not be specified; available CLASSPATH will be searched for the properties file)
	 * @return instance of ConnectionPoolManager relating to the specified properties file
	 * @throws IOException if there was an problem loading the properties
	 */
	public static synchronized ConnectionPoolManager getInstance(String propsFile) throws IOException
	{
		String s = propsFile.startsWith("/") ? propsFile : ("/" + propsFile);
		Object o = managers.get(s);
		ConnectionPoolManager cpm = (o != null) ? (ConnectionPoolManager)o : null;
		if (cpm == null  ||  cpm.isReleased())
		{
			cpm = new ConnectionPoolManager(loadProperties(s), propsFile);
			cpm.key = s;
			managers.put(cpm.key, cpm);
		}
		cpm.clients++;
		return cpm;
	}

	/**
	 * Returns the singleton instance of the ConnectionPoolManager for the specified properties file.
	 * @param propsFile filename of the properties file to use (path info should not be specified; available CLASSPATH will be searched for the properties file)
	 * @return instance of ConnectionPoolManager relating to the specified properties file
	 * @throws IOException if there was an problem loading the properties
	 */
	public static synchronized ConnectionPoolManager getInstance(File propsFile) throws IOException
	{
		Object o = managers.get(propsFile);
		ConnectionPoolManager cpm = (o != null) ? (ConnectionPoolManager)o : null;
		if (cpm == null  ||  cpm.isReleased())
		{
			try
			{
				cpm = new ConnectionPoolManager(loadProperties(propsFile), propsFile);
				cpm.key = propsFile;
				managers.put(cpm.key, cpm);
			}
			catch (IOException ioe)
			{
				if (ioe instanceof FileNotFoundException)
					System.err.println("Unable to find the properties file " + propsFile.getAbsolutePath());
				else
					System.err.println("Error loading the properties file " + propsFile.getAbsolutePath());
				ioe.printStackTrace();
				return null;
			}
		}
		cpm.clients++;
		return cpm;
	}

	/**
	 * Returns the standard singleton instance of the ConnectionPoolManager.
	 * If an instance has been obtained with a user-specified Properties object
	 * then this instance is returned, otherwise an attempt is made to return an
	 * instance using the default properties file (dbpool.properties).
	 * @throws IOException if there was an problem loading the properties
	 */
	public static synchronized ConnectionPoolManager getInstance() throws IOException
	{
		Object o = managers.get(PROPERTIES_INSTANCE_KEY);
		ConnectionPoolManager cpm = (o != null) ? (ConnectionPoolManager)o : null;
		if (cpm != null  &&  !cpm.released)
			cpm.clients++;
		else
			cpm = getInstance(DEFAULT_PROPERTIES_FILE);

		return cpm;
	}

	/**
	 * Creates a singleton instance of the ConnectionPoolManager for the specified
	 * Properties object. To subsequently use this instance user's should call the
	 * getInstance() method. This mechanism is used to provide the maximum
	 * separation between creation and use of this instance to avoid haphazard
	 * changes to any referenced Properties onject that may occur between calls.
	 * (This method can only be used successfully if no default properties
	 * instance exists and is in use at the time of calling.)
	 * @param props Properties object to use
	 * @throws RuntimeException if default properties instance already exists and is in use
	 */
	public static synchronized void createInstance(Properties props)
	{
		// Check for presence of default properties file instance
		Object o = managers.get(DEFAULT_PROPERTIES_FILE);
		ConnectionPoolManager cpm = (o != null) ? (ConnectionPoolManager)o : null;
		if (cpm != null  &&  !cpm.isReleased())
			throw new RuntimeException("Default properties file instance already exists");

		// Create new instance and store reference
		cpm = new ConnectionPoolManager(props, props);
		cpm.key = PROPERTIES_INSTANCE_KEY;
		managers.put(cpm.key, cpm);
	}

	/**
	 * Loads and returns a Properties object from file.
	 */
	private static Properties loadProperties(File propsFile) throws IOException
	{
		if (!propsFile.exists())
			throw new FileNotFoundException(propsFile.getAbsolutePath() + " does not exist");
		if (propsFile.isDirectory())
			throw new IOException("Error accessing properties file - " + propsFile.getAbsolutePath() + " is a directory");
		InputStream is = new FileInputStream(propsFile);
		Properties props = new Properties();
		props.load(is);
		is.close();
		return props;
	}

	/**
	 * Loads and returns a Properties object from the resource specified..
	 * The resource should be located in the current CLASSPATH to be found.
	 * @throws IOException if there was an problem loading the properties
	 */
	private static Properties loadProperties(String propsResource) throws IOException
	{
		InputStream is = ConnectionPoolManager.class.getResourceAsStream(propsResource);
		Properties props = new Properties();
		try
		{
			props.load(is);
		}
		catch (IOException ioe)
		{
			System.err.println("Unable to load the properties file. Make sure " + propsResource + " is in the CLASSPATH.");
			ioe.printStackTrace();
			throw ioe;
		}
		return props;
	}

	/**
	 * Initializes this instance with values from the given Properties object.
	 */
	private void init(Properties props)
	{
		String logFile = props.getProperty("logfile", "ConnectionPoolManager.log");
		String df = props.getProperty("dateformat", "EEE MMM dd hh:mm:ss.SSS ZZZ yyyy");
		try
		{
			setDateFormat(new SimpleDateFormat(df));
			setLog(new FileOutputStream(logFile, true));
		}
		catch (IOException e)
		{
			System.err.println("Can't open the log file: " + logFile);
		}
		loadDrivers(props);
		createPools(props);
	}

	/**
	 * Loads and registers all JDBC drivers. This is done by the
	 * DBConnectionManager, as opposed to the ConnectionPool,
	 * since many pools may share the same driver.
	 * @param props the connection pool properties
	 */
	private void loadDrivers(Properties props)
	{
		String driverClasses = props.getProperty("drivers");
		StringTokenizer st = new StringTokenizer(driverClasses, ",: \t\n\r\f");
		Enumeration current = DriverManager.getDrivers();
		while (st.hasMoreElements())
		{
			String driverClassName = st.nextToken().trim();
			try
			{
				// Check if driver already registered
				boolean using = false;
				while (current.hasMoreElements())
				{
					String cName = current.nextElement().getClass().getName();
					if (cName.equals(driverClassName))
						using = true;
				}
				if (!using)
				{
					Driver driver = (Driver)Class.forName(driverClassName).newInstance();
					DriverManager.registerDriver(driver);
					drivers.add(driver);
					log("Registered JDBC driver " + driverClassName);
				}
			}
			catch (Exception e)
			{
				log("Unable to register JDBC driver: " + driverClassName + ", Exception: " + e);
			}
		}
	}

	/**
	 * Creates instances of ConnectionPool based on the properties.
	 * @param props the connection pool properties
	 */
	private void createPools(Properties props)
	{
		Iterator iter = props.keySet().iterator();
		while (iter.hasNext())
		{
			String name = (String)iter.next();
			if (name.endsWith(".url"))
			{
				String poolName = name.substring(0, name.lastIndexOf("."));
				String url = props.getProperty(poolName + ".url");
				if (url == null)
				{
					log("No URL specified for " + poolName);
					continue;
				}

				String user = props.getProperty(poolName + ".user");
				user = (user != null) ? user.trim() : user;
				String pass = props.getProperty(poolName + ".password");
				pass = (pass != null) ? pass.trim() : pass;
				String poolSize = props.getProperty(poolName + ".maxpool", "0").trim();
				String maxSize = props.getProperty(poolName + ".maxconn", "0").trim();
				String init = props.getProperty(poolName + ".init", "0").trim();
				String expiry = props.getProperty(poolName + ".expiry", "0").trim();
				String validator = props.getProperty(poolName + ".validator");
				String decoder = props.getProperty(poolName + ".decoder");
				String logFile = props.getProperty(poolName + ".logfile");
				String dateformat = props.getProperty(poolName + ".dateformat");
				validator = (validator != null) ? validator.trim() : validator;
				boolean noCache = props.getProperty(poolName + ".cache", "true").trim().equalsIgnoreCase("false");
				boolean async = props.getProperty(poolName + ".async", "false").trim().equalsIgnoreCase("true");
				boolean poolDebug = props.getProperty(poolName + ".debug", "false").trim().equalsIgnoreCase("true");

				// Construct properties object for pool if extra info supplied
				Properties poolProps = new Properties();
				String prefix = poolName + ".prop.";
				Iterator it = props.keySet().iterator();
				while (it.hasNext())
				{
					String s = (String)it.next();
					if (s.startsWith(prefix))
						poolProps.setProperty(s.substring(prefix.length()), props.getProperty(s));
				}
				if (!poolProps.isEmpty()  &&  user != null  &&  !user.equals(""))
				{
					poolProps.setProperty("user", user);
					poolProps.setProperty("password", pass);
				}
				else
					poolProps = null;

				// Validate poolsize
				int pSize, mSize, iSize, exp;
				try { pSize = Integer.valueOf(poolSize).intValue(); }
				catch (NumberFormatException nfe)
				{
					log("Invalid maxpool value " + poolSize + " for " + poolName);
					pSize = 0;
				}
				// Validate maxsize
				try { mSize = Integer.valueOf(maxSize).intValue(); }
				catch (NumberFormatException nfe)
				{
					log("Invalid maxconn value " + maxSize + " for " + poolName);
					mSize = 0;
				}
				// Validate init
				try { iSize = Integer.valueOf(init).intValue(); }
				catch (NumberFormatException nfe)
				{
					log("Invalid initsize value " + init + " for " + poolName);
					iSize = 0;
				}
				// Validate expiry
				try { exp = Integer.valueOf(expiry).intValue(); }
				catch (NumberFormatException nfe)
				{
					log("Invalid expiry value " + expiry + " for " + poolName);
					exp = 0;
				}

				// Validate pool size logic
				pSize = Math.max(pSize, 0);  // (ensure pSize >= 0)
				mSize = Math.max(mSize, 0);  // (ensure mSize >= 0)
				if (mSize > 0)  // (if mSize > 0, ensure mSize >= pSize)
					mSize = Math.max(mSize, pSize);
				iSize = Math.min(Math.max(iSize, 0), pSize);  // (ensure 0 <= iSize <= pSize)
				exp = Math.max(exp, 0);  // (ensure exp >= 0)

				// Create connection pool
				ConnectionPool pool = null;
				if (poolProps != null)
					pool = new ConnectionPool(poolName, pSize, mSize, (long)(exp * 1000), url, poolProps);
				else
					pool = new ConnectionPool(poolName, pSize, mSize, (long)(exp * 1000), url, user, pass);

				// Set custom date format, if applicable.
				try
				{
					DateFormat df = new SimpleDateFormat(dateformat);
					pool.setDateFormat(df);
				}
				catch (Exception e)
				{
					log("Invalid dateformat string specified: " + dateformat);
				}

				// Setup pool logging (pool-specific if specified, otherwise generic logfile)
				if (logFile != null  &&  !logFile.equals(""))
				{
					File f = new File(logFile);
					if (f.exists()  &&  f.isDirectory())
						log("Invalid logfile specified for pool " + poolName + " - specified file is a directory");
					else if (!f.exists()  &&  !f.mkdirs())
						log("Invalid logfile specified for pool " + poolName + " - cannot create file " + f.getAbsolutePath());
					try { pool.setLog(new FileOutputStream(f, true)); }
					catch (FileNotFoundException fnfe)
					{
						log(fnfe, "Invalid logfile specified for pool " + poolName);
						pool.setLog(getLogStream());
					}
				}
				else
					pool.setLog(getLogStream());

				if (poolDebug)
					log("Enabling debug info on pool " + poolName);
				pool.setDebug(poolDebug);
				if (noCache)
					log("Disabling caching on pool " + poolName);
				pool.setCaching(!noCache);
				if (async)
					log("Enabling asynchronous destruction on pool " + poolName);
				pool.setAsyncDestroy(async);

				// Setup connection validator for pool
				if (validator != null  &&  !validator.equals(""))
				{
					try
					{
						Object o = Class.forName(validator).newInstance();
						if (o instanceof ConnectionValidator)
							pool.setValidator((ConnectionValidator)o);
					}
					catch (Exception ex)
					{
						log("Unable to instantiate validator class for pool " + poolName + ": " + validator);
					}
				}

				// Setup password decoder for pool
				if (decoder != null  &&  !decoder.equals(""))
				{
					try
					{
						Object o = Class.forName(decoder).newInstance();
						if (o instanceof PasswordDecoder)
							pool.setPasswordDecoder((PasswordDecoder)o);
					}
					catch (Exception ex)
					{
						log("Unable to instantiate password decoder class for pool " + poolName + ": " + decoder);
					}
				}

				// Add new pool to collection, and show summary info
				synchronized(pools) { pools.put(poolName, pool); }
				String info = "pool=" + pool.getPoolSize() + ",max=" + pool.getMaxSize() + ",expiry=";
				info += pool.getExpiryTime() == 0 ? "none" : pool.getExpiryTime() + "ms";
				log("Initialized pool " + poolName + " (" + info + ")");

				// Setup initial connections in pool (spawns a thread)
				if (iSize > 0)
					pool.init(iSize);
			}
		}
	}

	/**
	 * Returns a connection pool.
	 * (This is only provided as a convenience method to allow fine-tuning in
	 * exceptional circumstances.)
	 * @param name pool name as defined in the properties file
	 * @return the pool or null
	 */
	public ConnectionPool getPool(String name)
	{
		if (released)
			throw new RuntimeException("Pool manager no longer valid for use");
		return (ConnectionPool)pools.get(name);
	}

	/**
	 * Returns all the current connection pools maintained by this manager.
	 * (This is only provided as a convenience method.)
	 * @return array of ConnectionPool objects
	 */
	public ConnectionPool[] getPools()
	{
		synchronized(pools)
		{
			return (ConnectionPool[])pools.values().toArray(new ConnectionPool[0]);
		}
	}

	/**
	 * Returns an open connection from the specified pool.
	 * If one is not available, and the max number of connections has not been
	 * reached, a new connection is created.
	 * @param name pool name as defined in the properties file
	 * @return a connection, or null if unable to obtain one
	 */
	public Connection getConnection(String name) throws SQLException
	{
		if (released)
			throw new RuntimeException("Pool manager no longer valid for use");

		ConnectionPool pool = (ConnectionPool)pools.get(name);
		if (pool != null)
			return pool.getConnection();
		return null;
	}

	/**
	 * Returns an open connection from the specified pool.
	 * If one is not available, and the max number of connections has not been
	 * reached, a new connection is created. If the max number has been
	 * reached, waits until one is available or the specified time has elapsed.
	 * @param name pool name as defined in the properties file
	 * @param time number of milliseconds to wait
	 * @return the connection or null
	 */
	public Connection getConnection(String name, long time) throws SQLException
	{
		if (released)
			throw new RuntimeException("Pool manager no longer valid for use");

		ConnectionPool pool = (ConnectionPool)pools.get(name);
		if (pool != null)
			return pool.getConnection(time);
		return null;
	}

	/**
	 * Releases all resources for this ConnectionPoolManager, and deregisters
	 * JDBC drivers if necessary. Any connections still in use are forcibly closed.
	 */
	public synchronized void release()
	{
		// Don't release if client still active
		if (--clients > 0)
			return;
		// Set released flag to prevent check-out of new items
		released = true;

		synchronized(pools)
		{
			for (Iterator it = pools.values().iterator(); it.hasNext();)
			{
				ConnectionPool pool = (ConnectionPool)it.next();
				pool.releaseForcibly();
			}
		}

		// Check if drivers can be deregistered (only 1 manager left)
		if (managers.size() == 1)
		{
			for (Iterator it = drivers.iterator(); it.hasNext();)
			{
				Driver driver = (Driver)it.next();
				try
				{
					DriverManager.deregisterDriver(driver);
					log("Deregistered JDBC driver " + driver.getClass().getName());
				}
				catch (SQLException sqle)
				{
					log(sqle, "Can't deregister JDBC driver: " + driver.getClass().getName());
				}
			}
		}
		// Remove this manager from those referenced
		managers.remove(this.key);

		// Close log
		super.close();
	}

	/**
	 * Returns whether this instance has been released (and therefore is unusable).
	 */
	public synchronized boolean isReleased() { return this.released; }

	/**
	 * Convenience method to set the validator class for all managed connection pools.
	 * @deprecated To be removed in a future release
	 */
	public synchronized void setValidator(ConnectionValidator cv)
	{
		synchronized(pools)
		{
			if (pools != null)
			{
				for (Iterator it = pools.values().iterator(); it.hasNext();)
					((ConnectionPool)it.next()).setValidator(cv);
			}
		}
	}

	/**
	 * Convenience method to override LogUtil method to set log for pools.
	 * @deprecated To be removed in a future release
	 */
	public void setLog(OutputStream out)
	{
		super.setLog(out);
		// Set log for all pools
		synchronized(pools)
		{
			if (pools != null)
			{
				for (Iterator it = pools.values().iterator(); it.hasNext();)
					((ConnectionPool)it.next()).setLog(out);
			}
		}
	}

	/**
	 * Convenience method to override LogUtil method to set log for pools.
	 * @deprecated To be removed in a future release
	 */
	public void setLog(PrintStream ps)
	{
		super.setLog(ps);
		// Set log for all pools
		synchronized(pools)
		{
			if (pools != null)
			{
				for (Iterator it = pools.values().iterator(); it.hasNext();)
					((ConnectionPool)it.next()).setLog(ps);
			}
		}
	}
}