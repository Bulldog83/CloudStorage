package ru.bulldog.cloudstorage.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

public class DBAuthService implements AuthService, Closeable {

	private final static Logger logger = LogManager.getLogger(DBAuthService.class);

	private final DataBaseService dbService;
	private final Map<String, UserData> users;

	public DBAuthService() throws RuntimeException {
		this.users = new HashMap<>();
		this.dbService = new DataBaseService();
		loadUsers();
	}

	private void loadUsers() {
		List<UserData> users = dbService.getUsers();
		users.forEach(user -> this.users.put(user.getEmail(), user));
	}

	@Override
	public Optional<UUID> getUserId(String email, String password) {
		email = email.toLowerCase();
		if (!users.containsKey(email)) return Optional.empty();
		if (users.get(email).getPassword().equals(password)) {
			return Optional.of(users.get(email).getUserId());
		}
		return Optional.empty();
	}

	@Override
	public Optional<UUID> registerUser(String email, String password, String nickname) {
		if (users.containsKey(email)) {
			return getUserId(email, password);
		}
		logger.debug("Try to register new user: " + email);
		UserData newUser = new UserData(email, password, nickname);
		if (dbService.saveUser(newUser)) {
			users.put(email, newUser);
			return Optional.of(newUser.getUserId());
		}
		return Optional.empty();
	}

	@Override
	public boolean deleteUser(String login) {
		if (!users.containsKey(login)) return false;
		UserData user = users.get(login);
		if (dbService.deleteUser(user)) {
			users.remove(login);
			return true;
		}
		return false;
	}

	@Override
	public void close() throws IOException {
		dbService.close();
	}
}
