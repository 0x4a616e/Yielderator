package de.jangassen.yielderator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static de.jangassen.yielderator.CheckedExceptionUtils.isNoMoreElementsException;
import static de.jangassen.yielderator.CheckedExceptionUtils.throwNoMoreElements;

public class Yielder<T> {
  private final ValueWrapper<T> NO_MORE_ELEMENTS = new ValueWrapper<>(null);

  private volatile boolean hasMore = true;
  private volatile boolean aborted = false;

  private final Consumer<Yielder<T>> consumer;
  private final AtomicBoolean isStarted = new AtomicBoolean(false);
  private final Semaphore iterationLock = new Semaphore(0);
  private CompletableFuture<ValueWrapper<T>> valueFuture = new CompletableFuture<>();

  Yielder(Consumer<Yielder<T>> consumer) {
    this.consumer = consumer;
  }

  public void yield(T value) {
    valueFuture.complete(new ValueWrapper<>(value));

    try {
      iterationLock.acquire();
    } catch (InterruptedException e) {
      yieldBreak();
    }

    if (aborted) {
      yieldBreak();
    }
  }

  public void yieldBreak() {
    setNoMoreElements();

    throwNoMoreElements();
  }

  T next() {
    if (isStarted.compareAndSet(false, true)) {
      start();
    } else {
      valueFuture = new CompletableFuture<>();
      iterationLock.release();
    }

    if (!hasMore) {
      throwNoMoreElements();
    }
    return getNextValue();
  }

  private void start() {
    ForkJoinPool.commonPool().execute(this::execute);
  }

  private void execute() {
    try {
      consumer.accept(this);
    } catch (Throwable e) {
      if (!isNoMoreElementsException(e)) {
        throw e;
      }
    } finally {
      setNoMoreElements();
    }
  }

  boolean hasMore() {
    return hasMore;
  }

  private T getNextValue() {
    return unwrapValue(getWrappedValue());
  }

  private T unwrapValue(ValueWrapper<T> valueWrapper) {
    if (valueWrapper == NO_MORE_ELEMENTS) {
      throwNoMoreElements();
    }
    return valueWrapper.getValue();
  }

  private ValueWrapper<T> getWrappedValue() {
    try {
      return valueFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      setNoMoreElements();
      throwNoMoreElements(e);
      return null; // never reached
    }
  }

  private void setNoMoreElements() {
    hasMore = false;

    if (valueFuture != null && !valueFuture.isDone()) {
      valueFuture.complete(NO_MORE_ELEMENTS);
    }
  }

  void stop() {
    aborted = true;
    setNoMoreElements();
    iterationLock.release();
  }
}
