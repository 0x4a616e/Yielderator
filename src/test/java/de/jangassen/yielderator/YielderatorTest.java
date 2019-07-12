package de.jangassen.yielderator;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YielderatorTest {

  @Test
  public void shouldNotStartEagerly() {
    IteratingTestConsumer testConsumer = new IteratingTestConsumer(0, 10);

    Stream<Integer> stream = Yielderator.stream(testConsumer);

    Assert.assertNotNull(stream);
    Assert.assertNull(testConsumer.getException());
    Assert.assertFalse(testConsumer.isStarted());
    Assert.assertFalse(testConsumer.isFinished());
    Assert.assertEquals(0, testConsumer.getCounter());
  }

  @Test
  public void shouldReturnAllElements() {
    IteratingTestConsumer testConsumer = new IteratingTestConsumer(0, 10);

    List<Integer> elements = Yielderator.stream(testConsumer).collect(Collectors.toList());

    Assert.assertNotNull(elements);
    Assert.assertNull(testConsumer.getException());
    Assert.assertTrue(testConsumer.isStarted());
    Assert.assertTrue(testConsumer.isFinished());
    Assert.assertEquals(10, testConsumer.getCounter());
    Assert.assertEquals(10, elements.size());
    Assert.assertArrayEquals(new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, elements.toArray());
  }

  @Test
  public void shouldNotCreateMoreElementsThanNeeded() {
    IteratingTestConsumer testConsumer = new IteratingTestConsumer(0, 10);

    List<Integer> elements = Yielderator.stream(testConsumer).limit(5).collect(Collectors.toList());

    Assert.assertNotNull(elements);
    Assert.assertNull(testConsumer.getException());
    Assert.assertTrue(testConsumer.isStarted());
    Assert.assertFalse(testConsumer.isFinished());
    Assert.assertEquals(4, testConsumer.getCounter());
    Assert.assertEquals(5, elements.size());
    Assert.assertArrayEquals(new Integer[] { 0, 1, 2, 3, 4 }, elements.toArray());
  }

  @Test
  public void shouldStopAfterYieldBreak() {
    IteratingTestConsumer testConsumer = new IteratingTestConsumer(0, 10);
    testConsumer.breakAfter(4);

    List<Integer> elements = Yielderator.stream(testConsumer).collect(Collectors.toList());

    Assert.assertNotNull(elements);
    Assert.assertNull(testConsumer.getException());
    Assert.assertTrue(testConsumer.isStarted());
    Assert.assertTrue(testConsumer.isFinished());
    Assert.assertEquals(4, testConsumer.getCounter());
    Assert.assertEquals(5, elements.size());
    Assert.assertArrayEquals(new Integer[] { 0, 1, 2, 3, 4 }, elements.toArray());
  }

  @Test
  public void shouldCleanupUnusedYielderator() throws ExecutionException, InterruptedException {
    IteratingTestConsumer testConsumer = new IteratingTestConsumer(0, 10);
    Yielderator.stream(testConsumer).limit(3).collect(Collectors.toList());

    Assert.assertFalse(testConsumer.isFinished());

    System.gc();
    YielderFinalizerService.getInstance().cleanupNow();

    Assert.assertTrue(testConsumer.waitToFinish().get());
  }
}
