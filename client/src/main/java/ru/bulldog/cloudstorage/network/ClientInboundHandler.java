package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

	private final static Logger logger = LogManager.getLogger(ClientInboundHandler.class);

	private final ClientNetworkHandler networkHandler;

	public ClientInboundHandler(ClientNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		SocketAddress address = ctx.channel().remoteAddress();
		logger.info("Connected to: " + address);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		SocketAddress address = ctx.channel().remoteAddress();
		logger.info("Disconnected from: " + address);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		DataBuffer buffer = new DataBuffer((ByteBuf) msg);
		Connection connection = ctx.channel().attr(Connection.SESSION_KEY).get();
		if (connection == null) connection = networkHandler.getConnection();
		if (connection.isFileConnection()) {
			networkHandler.handleFile((FileConnection) connection, buffer);
		} else {
			while (buffer.isReadable()) {
				Optional<Packet> optionalPacket = Packet.read(buffer);
				if (optionalPacket.isPresent()) {
					Packet packet = optionalPacket.get();
					ctx.fireChannelRead(packet);
					if (packet.getType() == Packet.PacketType.FILE) break;
				} else {
					ctx.fireChannelRead(buffer.toString(StandardCharsets.UTF_8));
					break;
				}
			}
		}
	}
}
