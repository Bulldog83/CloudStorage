package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import ru.bulldog.cloudstorage.gui.controllers.MainController;
import ru.bulldog.cloudstorage.network.packet.FileProgressPacket;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.util.List;

public class ClientPacketInboundHandler extends PacketInboundHandler {

	private final MainController controller;

	public ClientPacketInboundHandler(MainController controller) {
		this.controller = controller;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		switch (packet.getType()) {
			case FILES_LIST:
				handleFilesList((FilesListPacket) packet);
				break;
			case FILE_PROGRESS:
				handleFileProgress((FileProgressPacket) packet);
				break;
		}
	}

	private void handleFileProgress(FileProgressPacket packet) {
		double progress = packet.getProgress();
		if (progress < 1.0) {
			controller.setClientProgress(progress);
		} else {
			controller.resetClientProgress();
		}
	}

	private void handleFilesList(FilesListPacket packet) {
		List<String> names = packet.getNames();
		controller.refreshServerFiles(names);
	}
}
