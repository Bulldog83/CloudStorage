package ru.bulldog.cloudstorage.database;

import java.util.Optional;
import java.util.UUID;

class UserData {
	private UUID userId;
	private String email;
	private String password;
	private String nickname;

	public UserData(UUID userId, String email, String password, String nickname) {
		this.userId = userId;
		this.email = email;
		this.password = password;
		this.nickname = nickname;
	}

	public UserData(String email, String password, String nickname) {
		this(UUID.randomUUID(), email, password, nickname);
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}
