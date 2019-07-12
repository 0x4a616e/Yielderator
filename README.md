# Yielder

After working a while with C#, I pretty much liked the concept of generators using `yield return` and `yield break` to
provide content for `IEnumerable`s as in the following (admittedly very basic) example:

```C#
IEnumerable<int> getSequence() {
    long counter = 0;
    while(true) {
        yield return counter++;
    }
}
```

The key thing here is that the returned `IEnumerable` is evaluated _lazy_. As a result, calling `getSequence()` will
_not_ result in an endless loop and just create an instance of `IEnumerable`. The following call will just perform 5
iterations and add them to a list.

```C#
getSequence().Take(5).ToList()
```

What you can do in Java of course is to use `Stream.generate` as in the following example:

```Java
Stream.generate(new Supplier<Long>() {
  long counter = 0;

  @Override
  public Long get() {
    return counter++;
  }
});
```

This however always creates an infinite stream. The `IEnumerable` example from C# on the other hand will end 
automatically once the end of the method is reached or `yield break` is called explicitely.

Thus, I created this little POC to achieve more or less the same behaviour as in C#. This is how it looks like:

```Java
Stream<Long> stream = Yielderator.stream(yielder -> {
  long counter = 0;
  
  while (true) {
    yielder.yield(counter++);
  }
});

List<Long> collect = stream.limit(5).collect(Collectors.toList());
```

As in C#, the stream is evaluated lazy. Once the end of the lambda is reached, or `yielder.yieldBreak()` is called 
explicitly, the stream ends. After the collection to `List`, the lambda will have yielded exactly 5 elements and 
`counter` will be set to 4.

The key thing here is that the `while` loop will _only_ perform another iteration when the stream is advanced. To
achieve that, the lambda is executed on a background thread. `Lock`s are used to prevent the loop from continuing after
calling `yield.return` until the stream is advanced and the lock is released.

## Caveat

In order for `yielder.yieldBreak()` to work, it will throw a `Throwable` that is caught and handled by the iterator. 
Thus you shouldn't use `yieldBreak` inside of any `catch` block that catches `Throwable`; Catching `Exception` will work 
just fine.