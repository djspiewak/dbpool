package snaq.util;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Base class providing simple logging and debug functionality, which can
 * either be instantiated and used as a logging object, or can be sub-classed
 * to be used as an integral logging facility.
 * <p>When specifying an OutputStream for use by the Logger, make sure that none
 * of the &quot;standard&quot; streams are used (System.out, System.err) as
 * they will be closed either when the close() method is called or the Logger
 * terminates. To send log to the standard output stream simply call
 * setLogging(true) and leave the actual log unspecified.
 * @author Giles Winstanley
 */
public class LogUtil
{
	protected DateFormat dateFormat, ddf;
	protected PrintStream log;
	protected boolean logging = false;
	protected boolean debug = false;


	/**
	 * Creates a new Logger with logging disabled.
	 */
	public LogUtil()
	{
	}

	/**
	 * Creates a new Logger which writes to the specified file.
	 * @throws FileNotFoundException if specified file is a directory, or cannot
	 * be opened for some reason.
	 */
	public LogUtil(File f) throws FileNotFoundException
	{
		setLog(new FileOutputStream(f, true));
	}

	/**
	 * Sets the date formatter for the logging.
	 */
	public synchronized void setDateFormat(DateFormat df) { dateFormat = df; }

	/**
	 * Sets the log stream and enables logging.
	 */
	public synchronized void setLog(OutputStream out)
	{
		if (log != null)
			close();
		if (out != null)
			log = new PrintStream(out);
		logging = true;
	}

	/**
	 * Sets the log writer and enables logging.
	 */
	public synchronized void setLog(PrintStream ps)
	{
		if (log != null)
			close();
		log = ps;
		logging = true;
	}

	/**
	 * Returns the current PrintStream used to write to the log.
	 */
	public PrintStream getLogStream()
	{
		return log;
	}

	/**
	 * Writes a message to the log, with an optional prefix.
	 */
	public synchronized void log(String prefix, String logEntry)
	{
		if (!logging)
			return;
		StringBuffer sb = new StringBuffer();
		if (dateFormat != null)
			sb.append(dateFormat.format(new Date()));
		else
		{
			if (ddf == null)
				ddf = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
			sb.append(ddf.format(new Date()));
		}

		sb.append(": ");
		if (prefix != null)
			sb.append(prefix);
		sb.append(logEntry);
		if (log != null)
		{
			synchronized(log)
			{
				log.println(sb.toString());
//				log.flush();
			}
		}
	}

	/**
	 * Writes a message to the log.
	 */
	public void log(String logEntry)
	{
		log("", logEntry);
	}

	/**
	 * Writes a message with an Exception to the log file.
	 */
	public void log(Throwable e, String prefix, String logEntry)
	{
		if (!logging)
			return;
		log(prefix, logEntry);
		e.printStackTrace(log);
		log.flush();
	}

	/**
	 * Writes a message with an Exception to the log file.
	 */
	public void log(Throwable e, String logEntry)
	{
		log(e, "", logEntry);
	}

	/**
	 * Writes an Exception to the log file.
	 */
	public void log(Throwable e)
	{
		log(e, e.getMessage());
	}

	/**
	 * Closes the log.
	 */
	public synchronized void close()
	{
		logging = false;
		if (log != null)
		{
			log.flush();
			if (!isSystemLog())
				log.close();
		}
		log = null;
	}

	// Returns whether the log is writing to a system log stream.
	private boolean isSystemLog()
	{
		return (!log.equals(System.out)  &&  !log.equals(System.err));
	}

	/**
	 * Determines whether to write to the log.
	 */
	public void setLogging(boolean b) { logging = b; }

	/**
	 * Returns whether logging is enabled.
	 */
	public boolean isLogging() { return logging; }

	/**
	 * Determines whether to perform debug logging.
	 */
	public void setDebug(boolean b) { debug = b; }

	/**
	 * Returns whether debug logging is enabled.
	 */
	public boolean isDebug() { return debug; }
}