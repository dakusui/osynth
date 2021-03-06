package com.github.dakusui.osynth.perf;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.utils.UtBase;
import org.junit.Test;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;

public class Perf extends UtBase {
  interface Acc {
    Integer increment(Integer value);
  }

  interface AccDefault extends Acc {
    default Integer increment(Integer value) {
      return value + 1;
    }
  }

  @Test
  public void a_testWithSynthesizedObject() {
    perform(synthesizeObjectWithMethodHandler());
  }

  @Test
  public void a_testWithSynthesizedObject2() {
    perform(synthesizeObjectWithHandlerObject());
  }

  @Test
  public void a_testWithSynthesizedObject3() {
    perform(synthesizeObjectWithDefaultMethod());
  }

  @Test
  public void b_testWithPlainObject() {
    perform(createObject());
  }

  public void perform(Acc acc) {
    long before = System.nanoTime();
    long p = before;
    int session = 0;
    Integer i;
    for (i = 0; i <= blockSize() * numSessions(); i = acc.increment(i)) {
      if (i % blockSize() == 0 && i > 0) {
        long now = System.nanoTime();
        printTime(session, (now - p));
        p = now;
        session++;
      }
    }
    long time = System.nanoTime() - before;
    printTime("total", time);
    System.out.println("result:" + i);
  }

  protected int numSessions() {
    return 10;
  }

  protected int blockSize() {
    return 100_000;
  }

  protected void printTime(Object label, long time) {
    System.out.println(label + ":" + time / 1_000_000 + "[msec]");
  }

  Acc createObject() {
    return value -> value + 1;
  }

  Acc synthesizeObjectWithMethodHandler() {
    return new ObjectSynthesizer()
        .addInterface(Acc.class)
        .handle(methodCall("increment", Integer.class).with((self, args) -> ((Integer) args[0]) + 1))
        .synthesize(Acc.class);
  }

  Acc synthesizeObjectWithHandlerObject() {
    return new ObjectSynthesizer()
        .addInterface(Acc.class)
        .addHandlerObject((Acc) value -> value + 1)
        .synthesize(Acc.class);
  }

  Acc synthesizeObjectWithDefaultMethod() {
    return new ObjectSynthesizer()
        .addInterface(AccDefault.class)
        .synthesize(Acc.class);
  }
}
