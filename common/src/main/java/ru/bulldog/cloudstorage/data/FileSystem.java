package ru.bulldog.cloudstorage.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public final class FileSystem {

	private final static Logger logger = LogManager.getLogger(FileSystem.class);
	private final static FileAttribute<Set<PosixFilePermission>> FOLDER_ATTRIBUTES;

	public final static String PATH_DELIMITER;

	private FileSystem() {}

	public static Path createFolder(Path folder, String name) {
		try {
			return Files.createDirectory(folder.resolve(name), FOLDER_ATTRIBUTES);
		} catch (IOException ex) {
			logger.error("Create folder error: " + folder, ex);
			return folder;
		}
	}

	public static boolean renameFile(Path folder, String oldName, String newName) {
		File oldFile = folder.resolve(oldName).toFile();
		File newFile = folder.resolve(newName).toFile();
		return oldFile.renameTo(newFile);
	}

	public static boolean deleteFile(Path filePath) {
		try {
			File toDelete = filePath.toFile();
			if (toDelete.exists() && toDelete.isDirectory()) {
				Files.list(filePath).forEach(FileSystem::deleteFile);
			}
			return Files.deleteIfExists(filePath);
		} catch (IOException ex) {
			logger.error("Delete file error: " + filePath, ex);
			return false;
		}
	}

	static {
		FOLDER_ATTRIBUTES = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r-----"));
		PATH_DELIMITER = FileSystems.getDefault().getSeparator();
	}
}
