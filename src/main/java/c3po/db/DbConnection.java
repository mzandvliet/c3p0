package c3po.db;

import java.net.InetSocketAddress;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a wrapper around the database connection which contains
 * the login credentials and can reinstate his connection
 * after a failure.
 * 
 * There are two methods that can query:
 *  SELECT - executeQueryWithRetries
 *  INS/UPD/DEL - executeStatementWithRetries
 */
public class DbConnection {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DbConnection.class);

	private final InetSocketAddress host;
	private final String user;
	private final String pwd;
	
	private Connection connection = null;
	
	/**
	 * @param host
	 * @param user
	 * @param pwd
	 */
	public DbConnection(InetSocketAddress host, String user, String pwd) {
		this.host = host;
		this.user = user;
		this.pwd = pwd;		
		
		// This will load the MySQL driver, each DB has its own driver
	    try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			LOGGER.error("Could not load database driver", e);
		}
	}

	public boolean open() {
		// Setup the connection with the DB
		try {
			connection = DriverManager.getConnection("jdbc:mysql://"+host.getHostName()+":"+host.getPort()+"/c3po?user="+user+"&password="+pwd);
			return true;
		} catch (SQLException e) {
			LOGGER.error("Could not open connection", e);
			return false;
		}
	}
	

	public boolean close() {
		// Close the connection with the DB
		try {
			connection.close();
			return true;
		} catch (SQLException e) {
			LOGGER.error("Could not close connection", e);
			return false;
		}
	}
	
	protected void reconnect() {
		LOGGER.debug("Attempting to reconnect...");
		close(); // First, attempt to clean up the old connection to avoid any leaks
		open(); // Then, open up a new connection
	}
	
	public boolean executeStatementWithRetries(String sql, int maxRetries) {
		int i = 0;
		
		while (i < maxRetries) {
			if (executeStatement(sql)) {
				return true;
			} else {
				LOGGER.debug(String.format("Retrying statement for the %snd time.", i + 1));
				reconnect();
			}
			i++;
		}
		
		LOGGER.error(String.format("Failed to execute statement after %s tries.", maxRetries + 1));
		return false;
	}
	
	private boolean executeStatement(String sql) {
		Statement statement = null;
		try {
			//LOGGER.debug("Executing: " + sql);
			statement = connection.createStatement();
			statement.execute(sql);
			return true;
		} catch (SQLException e) {
			LOGGER.error("Failed to execute statement", e);
			return false;
		}			
	}
	
	public ResultSet executeQueryWithRetries(String sql, int maxRetries) {
		int i = 0;
		
		while (i < maxRetries) {
			ResultSet resultset = executeQuery(sql);
			if (resultset != null) {
				return resultset;
			} else {
				reconnect();
				LOGGER.debug(String.format("Retrying statement for the %snd time.", i + 1));
			}
			i++;
		}
		
		LOGGER.error(String.format("Failed to execute query after %s tries.", maxRetries + 1));
		return null;
	}
	
	private ResultSet executeQuery(String sql) {
		Statement statement = null;
		try {
			//LOGGER.debug("Executing: " + sql);
			statement = connection.createStatement();
			return statement.executeQuery(sql);
		} catch (SQLException e) {
			LOGGER.error("Failed to execute query", e);
			return null;
		}			
	}
}
