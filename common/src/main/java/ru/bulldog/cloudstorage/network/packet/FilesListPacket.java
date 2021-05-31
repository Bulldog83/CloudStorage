package ru.bulldog.cloudstorage.network.packet;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import ru.bulldog.cloudstorage.data.DataBuffer;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FilesListPacket extends Packet {

	private final static String delimiter = ":";

	private List<String> names = Lists.newArrayList();

	public FilesListPacket(UUID session) {
		super(PacketType.FILES_LIST, session);
	}

	protected FilesListPacket(DataBuffer buffer) {
		super(PacketType.FILES_LIST, buffer);
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
