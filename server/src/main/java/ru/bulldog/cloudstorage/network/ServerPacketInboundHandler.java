package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;

public class ServerPacketInboundHandler extends PacketInboundHandler {
	private final static Logger logger = LogManager.getLogger(ServerPacketInboundHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		logger.debug("Received: " + packet.getType());
		switch (packet.getType()) {
			case LIST_REQUEST:
				ctx.writeAndFlush(new FilesListPacket());
				break;
		}
	}
}
