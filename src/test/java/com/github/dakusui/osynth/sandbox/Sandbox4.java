package com.github.dakusui.osynth.sandbox;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class Sandbox4 {
  @Test
  public void testComputeIfAbsent() {
    Map<String, String> map = new HashMap<>();
    map.put("hello", "WORLD");
    System.out.println(map.computeIfAbsent("hello", k -> k + ":world"));
    System.out.println(map.computeIfAbsent("hello!", k -> k + ":world"));
    System.out.println(map.toString().replaceAll(",", "\n"));
  }
}
