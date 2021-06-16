package ru.bulldog.cloudstorage.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public final class FileSystem {

	private final static Logger logger = LogManager.getLogger(FileSystem.class);

	public final static String PATH_DELIMITER;

	private FileSystem() {}

	public static Path createFolder(Path folder, String name) {
		try {
			Path newFolder = folder.resolve(name);
			if (newFolder.toFile().mkdir()) {
				return newFolder;
			}
			return folder;
		} catch (Exception ex) {
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
				Iterator<Path> pathIterator = Files.list(filePath).iterator();
				while (pathIterator.hasNext()) {
					Path path = pathIterator.next();
					if (!deleteFile(path)) {
						return false;
					}
				}
			}
			return Files.deleteIfExists(filePath);
		} catch (IOException ex) {
			logger.error("Delete file error: " + filePath, ex);
			return false;
		}
	}

	static {
		PATH_DELIMITER = FileSystems.getDefault().getSeparator();
	}
}
