package de.jangassen.yielderator;

class ValueWrapper<T> {
  private final T value;

  ValueWrapper(T value) {
    this.value = value;
  }

  T getValue() {
    return value;
  }
}
