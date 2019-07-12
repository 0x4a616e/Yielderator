package de.jangassen.yielderator;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

class YielderFinalizer extends PhantomReference<Object> {
  private final WeakReference<Yielder<?>> yielder;

  YielderFinalizer(Object referent, WeakReference<Yielder<?>> yielder, ReferenceQueue<? super Object> q) {
    super(referent, q);
    this.yielder = yielder;
  }

  void finalizeResources() {
    Yielder<?> yielder = this.yielder.get();
    if (yielder != null) {
      try {
        yielder.stop();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}