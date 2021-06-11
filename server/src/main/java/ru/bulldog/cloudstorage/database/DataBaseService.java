package ru.bulldog.cloudstorage.database;

import java.io.Closeable;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataBaseService implements Closeable {

	private final DataBase dataBase;

	public DataBaseService() throws RuntimeException {
		this.dataBase = DataBase.getInstance();
		if (dataBase == null) {
			throw new RuntimeException("Database error");
		}
	}

	public List<UserData> getUsers() {
		List<UserData> users = new ArrayList<>();
		String sql = "SELECT u.uuid, u.email, p.password, u.nickname FROM users AS u JOIN passwords AS p ON u.uuid = p.uuid";
		Statement statement = dataBase.getStatement();
		try {
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				users.add(new UserData(
					UUID.fromString(resultSet.getString("uuid")),
					resultSet.getString("email"),
					resultSet.getString("password"),
					resultSet.getString("nickname")
				));
			}
		} catch (SQLException ex) {
			DataBase.logger.error("Data load error", ex);
		}
		return users;
	}

	public boolean saveUser(UserData user) {
		try {
			boolean success;
			if (isUserExists(user.getEmail())) {
				String sql = "UPDATE users SET nickname=? WHERE email=?";
				try (PreparedStatement updateStatement = dataBase.prepareStatement(sql)) {
					updateStatement.setString(1, user.getNickname());
					updateStatement.setString(2, user.getEmail());
					success = updateStatement.executeUpdate() > 0;
				}
				sql = "UPDATE passwords SET password=? WHERE uuid=?";
				try (PreparedStatement updateStatement = dataBase.prepareStatement(sql)) {
					updateStatement.setString(1, user.getPassword());
					updateStatement.setString(2, user.getUserId().toString());
					success &= updateStatement.executeUpdate() > 0;
				}
			} else {
				String sql = "INSERT INTO users (uuid, email, nickname) VALUES (?, ?, ?)";
				try (PreparedStatement insertStatement = dataBase.prepareStatement(sql)) {
					insertStatement.setString(1, user.getUserId().toString());
					insertStatement.setString(2, user.getEmail());
					insertStatement.setString(3, user.getNickname());
					success = insertStatement.executeUpdate() > 0;
				}
				sql = "INSERT INTO passwords (uuid, password) VALUES (?, ?)";
				try (PreparedStatement insertStatement = dataBase.prepareStatement(sql)) {
					insertStatement.setString(1, user.getUserId().toString());
					insertStatement.setString(2, user.getPassword());
					success &= insertStatement.executeUpdate() > 0;
				}
			}
			return success;
		} catch (SQLException ex) {
			DataBase.logger.error("Database error", ex);
			return false;
		}
	}

	//TODO Change method
	public boolean deleteUser(UserData user) {
		if (user == null || !isUserExists(user.getEmail())) return false;
		String sql = "DELETE FROM users WHERE email=? LIMIT 1";
		try (PreparedStatement deleteStatement = dataBase.prepareStatement(sql)) {
			deleteStatement.setString(1, user.getEmail());
			return deleteStatement.executeUpdate() > 0;
		} catch (SQLException ex) {
			DataBase.logger.error("Database error", ex);
		}
		return false;
	}

	public boolean isUserExists(String email) {
		String sql = "SELECT email FROM users WHERE email=?";
		try (PreparedStatement statement = dataBase.prepareStatement(sql)) {
			statement.setString(1, email);
			ResultSet result = statement.executeQuery();
			return result.next();
		} catch (SQLException ex) {
			DataBase.logger.error("Database error", ex);
		}
		return false;
	}

	@Override
	public void close() throws IOException {
		dataBase.close();
	}
}
