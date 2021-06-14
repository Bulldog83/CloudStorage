package ru.bulldog.cloudstorage.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Deque;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ThreadManager extends ThreadPoolExecutor implements AutoCloseable {

	private final static Logger logger = LogManager.getLogger(ThreadManager.class);

	private final Deque<Runnable> workQueue = new ConcurrentLinkedDeque<>();
	private final AtomicBoolean closeImmediately = new AtomicBoolean(false);
	private final Object waitMonitor = new Object();
	private final Thread workingThread;
	private volatile boolean isWorking = false;
	private volatile boolean shutdown = false;

	public ThreadManager() {
		super(8, 8, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(8), task -> {
			if (task instanceof NamedTask) {
				NamedTask namedTask = (NamedTask) task;
				Thread work = new Thread(task, namedTask.getName());
				work.setDaemon(namedTask.isDaemon());
				return work;
			}
			return new Thread(task);
		}, (task, executor) -> {
			logger.debug("Task rejected: " + task);
			((ThreadManager) executor).executeFirst(task);
		});
		allowCoreThreadTimeOut(false);
		this.workingThread = new Thread(() -> {
			try {
				while (true) {
					if (workQueue.size() > 0) {
						super.execute(workQueue.poll());
					} else {
						pause();
					}
				}
			} catch (Exception ex) {
				logger.error("Tasks execution error", ex);
				closeImmediately();
				closeSilent();
			}
		}, "TaskManager");
		workingThread.setDaemon(true);
	}

	public void start() {
		if (isWorking) {
			throw new IllegalStateException("TaskManager already running.");
		}
		workingThread.start();
		isWorking = true;
	}

	@Override
	public void execute(Runnable command) {
		if (!shutdown) {
			logger.debug("Received new task: " + command);
			workQueue.offer(command);
		}
		resume();
	}

	protected void executeFirst(Runnable command) {
		if (!shutdown) {
			workQueue.offerFirst(command);
		}
		resume();
	}

	public <T> T complete(String taskName, Supplier<T> valueSupplier) {
		CompletableFuture<T> future = new CompletableFuture<>();
		execute(new NamedTask(taskName, () -> future.complete(valueSupplier.get()), false));
		return future.join();
	}

	public <T> T complete(Supplier<T> valueSupplier) {
		CompletableFuture<T> future = new CompletableFuture<>();
		execute(() -> future.complete(valueSupplier.get()));
		return future.join();
	}

	public void closeImmediately() {
		closeImmediately.set(true);
	}

	@Override
	public boolean isShutdown() {
		return shutdown;
	}

	public boolean isWorking() {
		return !shutdown && isWorking && workingThread.isAlive();
	}

	private void pause() throws InterruptedException {
		synchronized (waitMonitor) {
			waitMonitor.wait();
		}
	}

	private void resume() {
		synchronized (waitMonitor) {
			waitMonitor.notify();
		}
	}

	private void closeSilent() {
		try {
			close();
		} catch (Exception ignored) {}
	}

	@Override
	public void close() throws Exception {
		shutdown = true;
		if (closeImmediately.get()) {
			workQueue.clear();
			shutdownNow();
		} else {
			while (workQueue.size() > 0) {
				execute(workQueue.poll());
			}
			shutdown();
		}
	}
}
