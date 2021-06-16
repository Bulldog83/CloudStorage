package ru.bulldog.cloudstorage.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.FileInfo;
import ru.bulldog.cloudstorage.data.FileSystem;
import ru.bulldog.cloudstorage.network.handlers.PacketInboundHandler;
import ru.bulldog.cloudstorage.network.packet.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ServerPacketInboundHandler extends PacketInboundHandler {
	private final static Logger logger = LogManager.getLogger(ServerPacketInboundHandler.class);

	private final ServerNetworkHandler networkHandler;

	public ServerPacketInboundHandler(ServerNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		logger.debug("Received packet: " + packet);
		switch (packet.getType()) {
			case LIST_REQUEST:
				handleFilesListRequest(ctx, (ListRequest) packet);
				break;
			case FILE_REQUEST:
				handleFileRequest(ctx, (FileRequest) packet);
				break;
			case FILE:
				handleFilePacket(ctx, (FilePacket) packet);
				break;
			case AUTH_DATA:
				handleAuthData(ctx, (AuthData) packet);
				break;
			case SESSION:
				handleSessionPacket(ctx, (SessionPacket) packet);
				break;
			case REGISTRATION_DATA:
				handleNewUser(ctx, (RegistrationData) packet);
				break;
			case ACTION:
				handleActionPacket(ctx, (ActionPacket) packet);
				break;
		}
	}

	private void handleActionPacket(ChannelHandlerContext ctx, ActionPacket packet) {
		UUID sessionId = ctx.channel().attr(ChannelAttributes.SESSION_KEY).get();
		Session session = networkHandler.getSession(sessionId);
		if (session != null) {
			Path filesDir = session.getActiveFolder();
			switch (packet.getActionType()) {
				case FOLDER: {
					String folderName = packet.getName();
					Path newFolder = FileSystem.createFolder(filesDir, folderName);
					if (newFolder.equals(filesDir)) {
						ctx.writeAndFlush("Can't create directory: " + folderName);
					} else {
						ctx.writeAndFlush("Directory successfully created: " + folderName);
						ctx.writeAndFlush(new FilesListPacket());
					}
					break;
				}
				case RENAME:
					String fileName = packet.getName();
					String newName = packet.getNewName();
					if (FileSystem.renameFile(filesDir, fileName, newName)) {
						ctx.writeAndFlush(String.format("File %s successfully renamed: %s", fileName, newName));
						ctx.writeAndFlush(new FilesListPacket());
					} else {
						ctx.writeAndFlush("Can't rename file: " + fileName);
					}
					break;
				case DELETE:
					fileName = packet.getName();
					Path toDelete = filesDir.resolve(fileName);
					if (FileSystem.deleteFile(toDelete)) {
						ctx.writeAndFlush("File successfully deleted: " + fileName);
						ctx.writeAndFlush(new FilesListPacket());
					} else {
						ctx.writeAndFlush("Can't delete file: " + fileName);
					}
					break;
				default:
					ctx.writeAndFlush("Invalid action type.");
			}
		}
	}

	private void handleFilesListRequest(ChannelHandlerContext ctx, ListRequest packet) {
		UUID sessionId = ctx.channel().attr(ChannelAttributes.SESSION_KEY).get();
		Session session = networkHandler.getSession(sessionId);
		if (session != null) {
			Path filesDir = session.getActiveFolder();
			Path rootPath = session.getRootFolder();
			String requestPath = packet.getPath();
			if (packet.isParent()) {
				Path parentPath = filesDir.getParent();
				if (parentPath.equals(rootPath)) {
					filesDir = rootPath;
				} else {
					filesDir = parentPath;
				}
			} else {
				if (!requestPath.equals("")) {
					filesDir = filesDir.resolve(packet.getPath());
				}
			}
			session.setActiveFolder(filesDir);
			try {
				FilesListPacket response = new FilesListPacket();
				response.setFolder(filesDir.toString().replace(rootPath.toString(), "." + FileSystem.PATH_DELIMITER));
				List<FileInfo> filesNames = Files.list(filesDir).map(FileInfo::new)
						.collect(Collectors.toList());
				response.addAll(filesNames);
				ctx.writeAndFlush(response);
			} catch (Exception ex) {
				logger.error(ex.getLocalizedMessage(), ex);
				ctx.writeAndFlush("Folder not found.");
			}
		}
	}

	private void registerSession(ChannelHandlerContext ctx, UUID sessionId) {
		Channel channel = ctx.channel();
		networkHandler.registerSession(channel, sessionId);
		channel.attr(ChannelAttributes.SESSION_KEY).set(sessionId);
		ctx.writeAndFlush(new SessionPacket(sessionId));
		ctx.writeAndFlush(new FilesListPacket());
	}

	private void handleSessionPacket(ChannelHandlerContext ctx, SessionPacket packet) {
		Channel channel = ctx.channel();
		Session session = networkHandler.getSession(packet.getSession());
		if (session != null) {
			channel.attr(ChannelAttributes.SESSION_KEY).set(session.getSessionId());
		} else {
			ctx.writeAndFlush("No session found: " + packet.getSession());
			networkHandler.disconnect(channel.id());
		}
	}

	private void handleNewUser(ChannelHandlerContext ctx, RegistrationData packet) {
		Optional<UUID> uuidOptional = networkHandler.registerUser(packet.getEmail(), packet.getPassword(), packet.getNickname());
		if (uuidOptional.isPresent()) {
			registerSession(ctx, uuidOptional.get());
		} else {
			ctx.writeAndFlush("Email already exists or registration data wrong.");
			ctx.channel().close();
		}
	}

	private void handleAuthData(ChannelHandlerContext ctx, AuthData packet) {
		Optional<UUID> uuidOptional = networkHandler.getUserId(packet.getEmail(), packet.getPassword());
		if (uuidOptional.isPresent()) {
			registerSession(ctx, uuidOptional.get());
		} else {
			ctx.writeAndFlush("Invalid email or password.");
			ctx.channel().close();
		}
	}

	private void handleFilePacket(ChannelHandlerContext ctx, FilePacket packet) throws Exception {
		Channel channel = ctx.channel();
		UUID sessionId = packet.getSession();
		Session session = networkHandler.getSession(sessionId);
		if (session != null) {
			String fileName = packet.getName();
			File file = networkHandler.getFilesDir(sessionId).resolve(fileName).toFile();
			if (file.exists()) {
				logger.warn("File exists: " + file + ". Will recreate file.");
				file.delete();
			}
			ReceivingFile receivingFile = new ReceivingFile(file, packet.getSize());
			FileConnection fileConnection = new FileConnection(channel, sessionId, receivingFile);
			session.addFileChannel(channel.id(), fileConnection);
			channel.attr(ChannelAttributes.FILE_CHANNEL).set(true);
			networkHandler.handleFile(fileConnection, packet.getBuffer());
		} else {
			ctx.writeAndFlush("No registered session found.");
			channel.close();
		}
	}

	private void handleFileRequest(ChannelHandlerContext ctx, FileRequest packet) throws IOException {
		Channel channel = ctx.channel();
		channel.attr(ChannelAttributes.FILE_CHANNEL).set(true);
		String fileName = packet.getName();
		UUID sessionId = channel.attr(ChannelAttributes.SESSION_KEY).get();
		Optional<Path> filePath = Files.list(networkHandler.getFilesDir(sessionId))
				.filter(path -> fileName.equals(path.toFile().getName())).findFirst();
		if (filePath.isPresent()) {
			FilePacket filePacket = new FilePacket(packet.getSession(), filePath.get());
			ctx.writeAndFlush(filePacket);
		} else {
			Session session = networkHandler.getSession(sessionId);
			if (session != null) {
				session.getConnection().sendMessage("File not found.");
			}
			channel.close();
		}
	}
}
