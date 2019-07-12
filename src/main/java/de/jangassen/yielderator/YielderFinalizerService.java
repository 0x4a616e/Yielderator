package de.jangassen.yielderator;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class YielderFinalizerService {
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private final Lock lock = new ReentrantLock();
  private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
  private final List<YielderFinalizer> references = new ArrayList<>();

  private final int interval;
  private final TimeUnit unit;

  private ScheduledFuture<?> scheduledFuture;

  private static final YielderFinalizerService INSTANCE = new YielderFinalizerService(10, TimeUnit.SECONDS);

  private YielderFinalizerService(int interval, TimeUnit unit) {
    this.interval = interval;
    this.unit = unit;
  }

  public static YielderFinalizerService getInstance() {
    return INSTANCE;
  }

  private void start() {
    lock.lock();
    try {
      if (scheduledFuture == null) {
        scheduledFuture = scheduler.scheduleWithFixedDelay(this::cleanupNow, interval, interval, unit);
      }
    } finally {
      lock.unlock();
    }
  }

  private void stop() {
    lock.lock();
    try {
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
        scheduledFuture = null;
      }
    } finally {
      lock.unlock();
    }
  }

  void register(Object referent, Yielder<?> yielder) {
    references.add(new YielderFinalizer(referent, new WeakReference<>(yielder), referenceQueue));
    start();
  }

  public void cleanupNow() {
    Reference<?> referenceFromQueue;

    while ((referenceFromQueue = referenceQueue.poll()) != null) {
      YielderFinalizer reference = (YielderFinalizer) referenceFromQueue;
      reference.finalizeResources();
      reference.clear();
      references.remove(reference);
    }

    if (references.isEmpty()) {
      stop();
    }
  }

}
