package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

public class RegistrationData extends AuthData {

	private final String nickname;

	public RegistrationData(String email, String password, String nickname) {
		super(PacketType.USER_DATA, email, password);
		this.nickname = nickname;
	}

	public RegistrationData(DataBuffer buffer) {
		super(PacketType.USER_DATA, buffer);
		this.nickname = buffer.readString();
	}

	public String getNickname() {
		return nickname;
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeString(nickname);
	}
}
