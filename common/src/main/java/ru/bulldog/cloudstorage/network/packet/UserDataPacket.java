package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

public class UserDataPacket extends Packet {

	private final String email;
	private final String password;
	private final String nickname;

	public UserDataPacket(String email, String password, String nickname) {
		super(PacketType.USER_DATA);
		this.email = email;
		this.password = password;
		this.nickname = nickname;
	}

	public UserDataPacket(DataBuffer buffer) {
		super(PacketType.USER_DATA);
		this.email = buffer.readString();
		this.password = buffer.readString();
		this.nickname = buffer.readString();
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	public String getNickname() {
		return nickname;
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeString(email);
		buffer.writeString(password);
		buffer.writeString(nickname);
	}
}
