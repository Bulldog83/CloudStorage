package ru.bulldog.cloudstorage.event;

import com.google.common.collect.Sets;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;

import java.util.Set;

public final class EventsHandler implements ActionListener {

	private static EventsHandler instance;

	public static EventsHandler getInstance() {
		if (instance == null) {
			instance = new EventsHandler();
		}
		return instance;
	}

	private final Set<ActionListener> registeredListeners = Sets.newConcurrentHashSet();

	private EventsHandler() {}

	public void registerListener(ActionListener listener) {
		if (listener == this) {
			throw new IllegalArgumentException();
		}
		registeredListeners.add(listener);
	}

	public void removeListener(ActionListener listener) {
		registeredListeners.remove(listener);
	}

	@Override
	public void onConnect() {
		registeredListeners.forEach(ActionListener::onConnect);
	}

	@Override
	public void onDisconnect() {
		registeredListeners.forEach(ActionListener::onDisconnect);
	}

	@Override
	public void onMessageReceived(String message) {
		registeredListeners.forEach(listener -> listener.onMessageReceived(message));
	}

	@Override
	public void onHandleError(String message) {
		registeredListeners.forEach(listener -> listener.onHandleError(message));
	}

	@Override
	public void onFileReceived() {
		registeredListeners.forEach(ActionListener::onFileReceived);
	}

	@Override
	public void onFilesList(FilesListPacket filesList) {
		registeredListeners.forEach(listener -> listener.onFilesList(filesList));
	}

	@Override
	public void onFileStart(String direction, String fileName) {
		registeredListeners.forEach(listener -> listener.onFileStart(direction, fileName));
	}

	@Override
	public void onFileProgress(double progress) {
		registeredListeners.forEach(listener -> listener.onFileProgress(progress));
	}
}
