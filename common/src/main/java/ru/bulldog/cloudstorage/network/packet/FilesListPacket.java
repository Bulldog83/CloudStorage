package ru.bulldog.cloudstorage.network.packet;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

public class FilesListPacket extends Packet {

	private final List<String> names = Lists.newArrayList();

	public FilesListPacket() {
		super(PacketType.FILES_LIST);
	}

	public List<String> getNames() {
		return names;
	}

	public void addName(String name) {
		names.add(name);
	}

	public void addAll(Collection<String> names) {
		this.names.addAll(names);
	}
}
