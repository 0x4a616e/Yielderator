package de.jangassen.yielderator;

final class NoMoreElementsException extends Throwable {
  NoMoreElementsException() {
  }

  NoMoreElementsException(Throwable e) {
    super(e);
  }
}
