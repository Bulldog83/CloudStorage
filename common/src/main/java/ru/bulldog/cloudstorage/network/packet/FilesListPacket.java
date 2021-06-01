package ru.bulldog.cloudstorage.network.packet;

import com.google.common.collect.Lists;
import ru.bulldog.cloudstorage.data.DataBuffer;

import java.util.Collection;
import java.util.List;

public class FilesListPacket extends Packet {

	private final static String delimiter = ":";

	private List<String> names = Lists.newArrayList();

	public FilesListPacket() {
		super(PacketType.FILES_LIST);
	}

	protected FilesListPacket(DataBuffer buffer) {
		super(PacketType.FILES_LIST);
		String names = buffer.readString();
		this.names = Lists.newArrayList(names.split(delimiter));
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

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		String namesStr = String.join(delimiter, names);
		buffer.writeString(namesStr);
	}
}
