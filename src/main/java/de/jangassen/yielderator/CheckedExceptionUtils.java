package de.jangassen.yielderator;

public class CheckedExceptionUtils {

  static void throwNoMoreElements() {
    throwCheckedException(new NoMoreElementsException());
  }

  static void throwNoMoreElements(Throwable e) {
    throwCheckedException(new NoMoreElementsException(e));
  }

  static boolean isNoMoreElementsException(Throwable e) {
    return e instanceof NoMoreElementsException;
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void throwCheckedException(Throwable t) throws T {
    throw (T) t;
  }
}
