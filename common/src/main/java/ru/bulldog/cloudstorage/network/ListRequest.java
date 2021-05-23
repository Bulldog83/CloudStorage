package ru.bulldog.cloudstorage.network;

public class ListRequest extends Packet {

	public ListRequest() {
		super(PacketType.LIST_REQUEST);
	}
}
