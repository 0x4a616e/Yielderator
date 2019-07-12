package de.jangassen.yielderator;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static de.jangassen.yielderator.CheckedExceptionUtils.isNoMoreElementsException;

public class Yielderator<T> implements Spliterator<T> {
  private final Yielder<T> yielder;

  private Yielderator(Yielder<T> yielder) {
    this.yielder = yielder;
  }

  public static <T> Stream<T> stream(Consumer<Yielder<T>> consumer) {
    return new Yielderator<>(new Yielder<>(consumer)).stream();
  }

  private Stream<T> stream() {
    Stream<T> stream = StreamSupport.stream(this, false);
    YielderFinalizerService.getInstance().register(stream, yielder);
    return stream;
  }

  @Override
  public boolean tryAdvance(Consumer<? super T> action) {
    try {
      T next = yielder.next();
      action.accept(next);
      return yielder.hasMore();
    } catch (Throwable e) {
      if (isNoMoreElementsException(e)) {
        return false;
      }
      throw e;
    }
  }

  @Override
  public Spliterator<T> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return 0;
  }
}
