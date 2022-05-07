package com.github.dakusui.osynth2.compat.sandbox;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class Sandbox4 {
  @Test
  public void testComputeIfAbsent() {
    Map<String, String> map = new HashMap<>();
    map.put("hello", "WORLD");
    System.out.println(map.computeIfAbsent("hello", k -> k + ":world"));
    System.out.println(map.computeIfAbsent("hello!", k -> k + ":world"));
    System.out.println(map.toString().replaceAll(",", "\n"));
  }

  @Test
  public void givenHashMap_whenSumParallel_thenError() throws Exception {
    for (int i = 0; i < 100; i++) {
      Map<String, Integer> map = new HashMap<>();
      List<Integer> sumList = parallelSum100(map, 100);

      assertNotEquals(1, sumList
          .stream()
          .distinct()
          .count());
      long wrongResultCount = sumList
          .stream()
          .filter(num -> num != 100)
          .count();
      System.out.println(i);
      assertTrue(wrongResultCount > 0);
    }
  }

  @Test
  public void givenConcurrentMap_whenSumParallel_thenCorrect()
      throws Exception {
    for (int i = 0; i < 100; i++) {
      Map<String, Integer> map = new ConcurrentHashMap<>();
      List<Integer> sumList = parallelSum100(map, 1000);

      assertEquals(1, sumList
          .stream()
          .distinct()
          .count());
      long wrongResultCount = sumList
          .stream()
          .filter(num -> num != 100)
          .count();
      System.out.println(i);
      assertEquals(0, wrongResultCount);
    }
  }

  private List<Integer> parallelSum100(Map<String, Integer> map, int executionTimes) throws InterruptedException {
    List<Integer> sumList = new ArrayList<>(1000);
    for (int i = 0; i < executionTimes; i++) {
      map.put("test", 0);
      ExecutorService executorService = Executors.newFixedThreadPool(4);
      for (int j = 0; j < 10; j++) {
        executorService.execute(() -> {
          for (int k = 0; k < 10; k++)
            map.computeIfPresent(
                "test",
                (key, value) -> value + 1
            );
        });
      }
      executorService.shutdown();
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
        throw new RuntimeException("timeout!");
      sumList.add(map.get("test"));
    }
    return sumList;
  }
}
