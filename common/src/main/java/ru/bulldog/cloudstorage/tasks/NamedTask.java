package ru.bulldog.cloudstorage.tasks;

public class NamedTask implements Runnable {

	private final String name;
	private final Runnable work;
	private final boolean isDaemon;

	public NamedTask(String name, Runnable work, boolean isDaemon) {
		this.isDaemon = isDaemon;
		this.name = name;
		this.work = work;
	}

	public String getName() {
		return name;
	}

	public boolean isDaemon() {
		return isDaemon;
	}

	@Override
	public void run() {
		work.run();
	}

	@Override
	public String toString() {
		return "NamedTask{" + name + "}";
	}
}
