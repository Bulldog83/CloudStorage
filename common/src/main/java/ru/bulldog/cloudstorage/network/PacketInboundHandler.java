package ru.bulldog.cloudstorage.network;

import io.netty.channel.SimpleChannelInboundHandler;
import ru.bulldog.cloudstorage.network.packet.Packet;

public abstract class PacketInboundHandler extends SimpleChannelInboundHandler<Packet> {}
