package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.compat.ObjectSynthesizer;
import com.github.dakusui.osynth.compat.CompatSimpleObjectSynthesizer;
import com.github.dakusui.osynth.utils.UtBase;
import org.junit.Test;

import static com.github.dakusui.crest.Crest.*;

/**
 * A test class to fix a behavior reported as an issue #53 of floorplan library.
 * Base     Fallback    Synth     Expectation
 * method1  Absent      Absent      Absent    -
 * method2  Present     Absent      Absent    Base
 * method3  Absent      Present     Absent    Fallback
 * method4  Present     Present     Absent    Fallback
 * method5  Absent      Absent      Present   -
 * method6  Present     Absent      Present   Synth
 * method7  Absent      Present     Present   Synth
 * method8  Present     Present     Present   Synth
 */
public class CompatSimpleObjectSynthesizerFloorplanIssue53Test extends UtBase {
  public interface Base {
    default String method2() {
      return "Base:method2";
    }

    String method3();

    default String method4() {
      return "Base:method4";
    }

    default String method6() {
      return "Base:method6";
    }

    String method7();

    default String method8() {
      return "Base:method8";
    }
  }

  static abstract class FallbackBase implements Base {
    @Override
    public String method4() {
      return "FallbackBase:method4";
    }
  }

  @Test
  public void test() {
    Base base = CompatSimpleObjectSynthesizer.create(Base.class)
        .addHandlerObject(new FallbackBase() {
          @Override
          public String method3() {
            return "Fallback:method3";
          }

          @Override
          public String method7() {
            return "Fallback:method7";
          }

          @Override
          public String method8() {
            return "Fallback:method8";

          }
        })
        .handle(ObjectSynthesizer.methodCall("method6").with((object, args) -> "Synth:method6"))
        .handle(ObjectSynthesizer.methodCall("method7").with((object, args) -> "Synth:method7"))
        .handle(ObjectSynthesizer.methodCall("method8").with((object, args) -> "Synth:method8"))
        .synthesize();

    System.out.println(base.method2());
    System.out.println(base.method3());
    System.out.println(base.method4());
    System.out.println(base.method6());
    System.out.println(base.method7());
    System.out.println(base.method8());

    assertThat(
        base,
        allOf(
            asString("method2").equalTo("Base:method2").$(),
            asString("method3").equalTo("Fallback:method3").$(),
            asString("method4").equalTo("FallbackBase:method4").$(),
            asString("method6").equalTo("Synth:method6").$(),
            asString("method7").equalTo("Synth:method7").$(),
            asString("method8").equalTo("Synth:method8").$()
        )
    );
  }
}
