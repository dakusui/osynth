package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.MethodHandler;
import com.github.dakusui.osynth.ObjectSynthesizer;
import org.junit.Test;

public class Perf {
  interface Acc {
    Integer increment(Integer value);
  }

  @Test
  public void a_testWithSynthesizedObject() {
    perform(synthesizeObject());
  }

  @Test
  public void b_testWithPlainObject() {
    perform(createObject());
  }

  public void perform(Acc acc) {
    long before = System.nanoTime();
    long p = before;
    int session = 0;
    for (Integer i = 0; i < 10_000_000; i = acc.increment(i)) {
      if (i > 0 && i % 1_000_000 == 0) {
        long now = System.nanoTime();
        printTime(session, (now - p));
        p = now;
        session++;
      }
    }
    long time = System.nanoTime() - before;
    printTime("total", time);
  }

  protected void printTime(Object label, long time) {
    System.out.println(label + ":" + time / 1_000_000 + "[msec]");
  }

  Acc createObject() {
    return value -> value + 1;
  }

  Acc synthesizeObject() {
    return new ObjectSynthesizer()
        .addInterface(Acc.class)
        .handle(MethodHandler.builderByNameAndParameterTypes("increment", Integer.class).with((self, args) -> ((Integer) args[0]) + 1))
        .synthesize(Acc.class);
  }
}
