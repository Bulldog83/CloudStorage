package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

public class AuthData extends Packet {

	private final String login;
	private final String password;

	public AuthData(String login, String password) {
		super(PacketType.AUTH_DATA);
		this.login = login;
		this.password = password;
	}

	protected AuthData(DataBuffer buffer) {
		super(PacketType.AUTH_DATA);
		this.login = buffer.readString();
		this.password = buffer.readString();
	}

	public String getLogin() {
		return login;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeString(login)
			  .writeString(password);
	}
}
