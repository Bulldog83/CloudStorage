package ru.bulldog.cloudstorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Properties;

public final class Settings {

	private final static Logger logger = LogManager.getLogger(Settings.class);

	private final static Properties properties;
	private final static File propsFile;

	public final static String HOST;
	public final static int PORT;

	public static boolean isSaveAuthData() {
		return Boolean.parseBoolean(properties.getProperty("auth.save_data", "false"));
	}

	public static void setSaveAuthData(boolean value) {
		properties.setProperty("auth.save_data", String.valueOf(value));
		save();
	}

	public static String getAuthLogin() {
		return properties.getProperty("auth.login", "");
	}

	public static String getAuthPassword() {
		return properties.getProperty("auth.password", "");
	}

	public static void saveAuthLogin(String login) {
		properties.setProperty("auth.login", login);
		save();
	}

	public static void saveAuthData(String login, String password) {
		properties.setProperty("auth.login", login);
		properties.setProperty("auth.password", password);
		save();
	}

	private static void save() {
		try (OutputStream outputStream = new FileOutputStream(propsFile)) {
			properties.store(outputStream, null);
		} catch (Exception ex) {
			logger.error("Settings save error", ex);
		}
	}

	static {
		properties = new Properties();
		propsFile = new File("app.properties");
		if (propsFile.exists()) {
			try (InputStream inputStream = new FileInputStream(propsFile)) {
				properties.load(inputStream);
			} catch (Exception ex) {
				logger.error("Settings load error", ex);
			}
		}
		HOST = properties.getProperty("server.host", "localhost");
		PORT = (int) properties.getOrDefault("server.port", 8072);
	}
}
