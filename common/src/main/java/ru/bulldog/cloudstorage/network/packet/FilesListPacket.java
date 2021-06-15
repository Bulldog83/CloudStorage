package ru.bulldog.cloudstorage.network.packet;

import com.google.common.collect.Lists;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.data.FileInfo;

import java.util.Collection;
import java.util.List;

public class FilesListPacket extends Packet {

	private final List<FileInfo> files = Lists.newArrayList();
	private String folder;

	public FilesListPacket() {
		super(PacketType.FILES_LIST);
	}

	protected FilesListPacket(DataBuffer buffer) {
		super(PacketType.FILES_LIST);
		this.folder = buffer.readString();
		int count = buffer.readInt();
		for (int i = 0; i < count; i++) {
			files.add(buffer.readFileInfo());
		}
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public List<FileInfo> getFiles() {
		return files;
	}

	public void addAll(Collection<FileInfo> files) {
		this.files.addAll(files);
	}

	@Override
	public void write(DataBuffer buffer) throws Exception {
		super.write(buffer);
		buffer.writeString(folder);
		buffer.writeInt(files.size());
		files.forEach(buffer::writeFileInfo);
	}
}
