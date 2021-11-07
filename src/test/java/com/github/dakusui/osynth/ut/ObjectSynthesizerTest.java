package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.utils.UtBase;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class ObjectSynthesizerTest extends UtBase {
  public interface TestInterface {
    void method(List<String> out);

    default void method2(List<String> out) {
      out.add("DefaultImplementation");
    }
  }

  final List<String> out = new LinkedList<>();

  @Test
  public void test1() {
    TestInterface obj = ObjectSynthesizer.create().addInterface(TestInterface.class).synthesize().castTo(TestInterface.class);

    obj.method2(out);

    System.out.println(out);
  }

}
