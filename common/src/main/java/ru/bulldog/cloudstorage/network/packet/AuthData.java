package ru.bulldog.cloudstorage.network.packet;

import ru.bulldog.cloudstorage.data.DataBuffer;

public class AuthData extends Packet {

	private final String email;
	private final String password;

	public AuthData(String email, String password) {
		super(PacketType.AUTH_DATA);
		this.email = email;
		this.password = password;
	}

	protected AuthData(DataBuffer buffer) {
		super(PacketType.AUTH_DATA);
		this.email = buffer.readString();
		this.password = buffer.readString();
	}

	public AuthData(PacketType packetType, String email, String password) {
		super(packetType);
		this.email = email;
		this.password = password;
	}

	public AuthData(PacketType packetType, DataBuffer buffer) {
		super(packetType);
		this.email = buffer.readString();
		this.password = buffer.readString();
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeString(email)
			  .writeString(password);
	}
}
