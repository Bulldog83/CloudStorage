package ru.bulldog.cloudstorage.network.packet;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FilesListPacket extends Packet {

	private final static String delimiter = ":";

	private List<String> names = Lists.newArrayList();

	public FilesListPacket() {
		super(PacketType.FILES_LIST);
	}

	protected FilesListPacket(ByteBuf buffer) {
		super(PacketType.FILES_LIST);
		int len = buffer.readInt();
		String names = buffer.readCharSequence(len, StandardCharsets.UTF_8).toString();
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
	public void write(ByteBuf buffer) throws Exception {
		super.write(buffer);
		String names = String.join(delimiter, this.names);
		buffer.writeInt(names.length());
		buffer.writeBytes(names.getBytes(StandardCharsets.UTF_8));
	}
}
