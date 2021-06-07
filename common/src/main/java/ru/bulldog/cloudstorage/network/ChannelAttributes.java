package ru.bulldog.cloudstorage.network;

import io.netty.util.AttributeKey;

import java.util.UUID;

public final class ChannelAttributes {
	public final static AttributeKey<UUID> SESSION_KEY = AttributeKey.valueOf("session_id");
	public final static AttributeKey<Boolean> FILE_CHANNEL = AttributeKey.valueOf("file_channel");
}
