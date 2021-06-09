package ru.bulldog.cloudstorage.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.sql.*;

public class DataBase implements Closeable {

	public final static Logger logger = LogManager.getLogger(DataBase.class);
	private static DataBase instance;

	@Nullable
	public static DataBase getInstance() {
		try {
			if (instance == null) {
				instance = new DataBase();
			}
		} catch (Exception ex) {
			logger.error("Database initialization error.", ex);
		}
		return instance;
	}

	private final Connection connection;
	private final Statement statement;

	private DataBase() throws Exception {
		File dataDir = new File("data");
		if (!dataDir.exists() && !dataDir.mkdirs()) {
			throw new IOException("Can't create 'data' directory.");
		}

		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:data/data.sqlite");
		statement = connection.createStatement();

		try (InputStream resource = getClass().getResourceAsStream("data.sql")) {
			if (resource != null) {
				DataInputStream dataInputStream = new DataInputStream(resource);
				byte[] bytes = new byte[dataInputStream.available()];
				dataInputStream.read(bytes);
				String initSql = new String(bytes);
				statement.execute(initSql);
			}
		} catch (Exception ex) {
			logger.error("Data base initialization error.", ex);
		}
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return connection.prepareStatement(sql);
	}

	public Statement getStatement() {
		return statement;
	}

	public void enableAutocommit() throws SQLException {
		connection.setAutoCommit(true);
	}

	public void disableAutocommit() throws SQLException {
		connection.setAutoCommit(false);
	}

	@Override
	public void close() throws IOException {
		try {
			statement.close();
			connection.close();
		} catch (SQLException ex) {
			logger.error("Database connection close error", ex);
		}
	}
}
