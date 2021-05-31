package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import javafx.application.Platform;
import ru.bulldog.cloudstorage.gui.controllers.MainController;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.util.List;

public class ClientPacketInboundHandler extends PacketInboundHandler {

	private MainController controller;

	public ClientPacketInboundHandler(MainController controller) {
		this.controller = controller;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		switch (packet.getType()) {
			case FILES_LIST:
				handleFilesList((FilesListPacket) packet);
				break;
		}
	}

	private void handleFilesList(FilesListPacket packet) {
		List<String> names = packet.getNames();
		Platform.runLater(() -> controller.serverFiles.getItems().setAll(names));
	}
}
