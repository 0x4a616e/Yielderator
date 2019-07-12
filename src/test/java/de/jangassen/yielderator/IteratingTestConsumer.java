package de.jangassen.yielderator;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

class IteratingTestConsumer implements Consumer<Yielder<Integer>> {
  private final int from;
  private final int to;
  private int counter = 0;
  private Integer breakAfter;
  private boolean started;
  private boolean finished;
  private Exception exception;

  IteratingTestConsumer(int from, int to) {
    this.from = from;
    this.to = to;
  }

  public void accept(Yielder<Integer> yielder) {
    try {
      started = true;
      try {
        for (counter = from; counter < to; counter++) {
          yielder.yield(counter);

          if (breakAfter != null && breakAfter == counter) {
            yielder.yieldBreak();
          }
        }
      } finally {
        finished = true;
      }
    } catch (Exception e) {
      this.exception = e;
    }
  }

  void breakAfter(int value) {
    breakAfter = value;
  }

  boolean isStarted() {
    return started;
  }

  boolean isFinished() {
    return finished;
  }

  int getCounter() {
    return counter;
  }

  Exception getException() {
    return exception;
  }

  CompletableFuture<Boolean> waitToFinish() {
    return CompletableFuture.supplyAsync(() -> {
      for (int i = 0; i < 10; i++) {
        if (isFinished()) {
          return true;
        }
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      return isFinished();
    });
  }
}
