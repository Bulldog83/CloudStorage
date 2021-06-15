package ru.bulldog.cloudstorage.data;

import java.io.File;
import java.nio.file.Path;

public class FileInfo {

	private final String fileName;
	private final long fileSize;
	private final boolean isDirectory;

	private File sourceFile;

	public FileInfo(String fileName, long fileSize, boolean isDirectory) {
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.isDirectory = isDirectory;
	}

	public FileInfo(Path source) {
		this.sourceFile = source.toFile();
		this.fileName = source.getFileName().toString();
		this.fileSize = sourceFile.length();
		this.isDirectory = sourceFile.isDirectory();
	}

	public String getFileName() {
		return fileName;
	}

	public long getFileSize() {
		return fileSize;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public File getSourceFile() {
		return sourceFile;
	}
}
