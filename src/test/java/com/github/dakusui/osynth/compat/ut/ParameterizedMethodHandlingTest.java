package com.github.dakusui.osynth.compat.ut;

import com.github.dakusui.osynth.compat.testwrappers.LegacyObjectSynthesizer;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import org.junit.Test;

import java.util.Arrays;

import static com.github.dakusui.crest.Crest.*;
import static com.github.dakusui.osynth.compat.testwrappers.LegacyObjectSynthesizer.methodCall;

public class ParameterizedMethodHandlingTest extends UtBase {
  interface Single {
    default String addIndex(int index) {
      return "HELLO_INT_" + index;
    }

    default String addIndex(long index) {
      return "HELLO_LONG_" + index;
    }
  }

  interface Multi {
    default String addIndex(String string, int index) {
      return string + "_" + index;
    }
  }

  @Test
  public void testSingle() {
    Single x = new LegacyObjectSynthesizer().addInterface(Single.class).synthesize().castTo(Single.class);
    assertThat(
        x.addIndex(99) + ":" + x.addIndex(100L),
        asString().equalTo("HELLO_INT_99:HELLO_LONG_100").$());
  }

  @Test
  public void testSingle_2() {
    Single x = new LegacyObjectSynthesizer()
        .addInterface(Single.class)
        .handle(methodCall("addIndex", int.class).with((self, args) -> "handleInt:" + Arrays.toString(args)))
        .handle(methodCall("addIndex", long.class).with((self, args) -> "handleLong:" + Arrays.toString(args)))
        .synthesize()
        .castTo(Single.class);
    assertThat(
        x,
        allOf(
            asString("addIndex", 99).equalTo("handleInt:[99]").$(),
            asString("addIndex", 100L).equalTo("handleLong:[100]").$()));
  }

  @Test
  public void testDouble() {
    Multi x = new LegacyObjectSynthesizer().addInterface(Multi.class).synthesize().castTo(Multi.class);
    assertThat(
        x.addIndex("hello", 100),
        asString().equalTo("hello_100").$());
  }
}
