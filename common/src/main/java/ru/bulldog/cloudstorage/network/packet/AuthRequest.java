package ru.bulldog.cloudstorage.network.packet;

public class AuthRequest extends Packet {

	protected AuthRequest() {
		super(PacketType.AUTH_REQUEST);
	}
}
