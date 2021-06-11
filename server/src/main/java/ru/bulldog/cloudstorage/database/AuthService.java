package ru.bulldog.cloudstorage.database;

import java.util.Optional;
import java.util.UUID;

public interface AuthService {
	Optional<UUID> getUserId(String login, String password);
	Optional<UUID> registerUser(String email, String password, String nickname);
	boolean deleteUser(String login);
}
