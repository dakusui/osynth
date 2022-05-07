package com.github.dakusui.osynth.compat.ut;

import com.github.dakusui.osynth.compat.SimpleObjectSynthesizer;
import com.github.dakusui.osynth.compat.utils.UtBase;
import org.junit.Test;

import static com.github.dakusui.crest.Crest.*;
import static com.github.dakusui.osynth.compat.SimpleObjectSynthesizer.methodCall;

/**
 * A test class to fix a behavior reported as an issue #53 of floorplan library.
 * // <pre>
 * ----
 * -        Synth       Base IF     Fallback    Expectation
 * method1  Absent      Absent      Absent      COMPILATION ERROR
 * method2  Absent      Present     Absent      Base
 * method3  Absent      Absent      Present     Fallback
 * method4  Absent      Present     Present     Fallback -> Base
 * method5  Present     Absent      Absent      COMPILATION ERROR
 * method6  Present     Present     Absent      Synth
 * method7  Present     Absent      Present     Synth
 * method8  Present     Present     Present     Synth
 * ----
 * // </pre>
 */
public class SimpleObjectSynthesizerFloorplanIssue53Test extends UtBase {
  public interface Base {
//    String method1();

    default String method2() {
      return "Base:method2";
    }

    String method3();

    default String method4() {
      return "Base:method4";
    }

//    String method5();

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
    Base base = SimpleObjectSynthesizer.create(Base.class)
        .fallbackTo(new FallbackBase() {
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
        .handle(methodCall("method5").with((object, args) -> "Synth:method5"))
        .handle(methodCall("method6").with((object, args) -> "Synth:method6"))
        .handle(methodCall("method7").with((object, args) -> "Synth:method7"))
        .handle(methodCall("method8").with((object, args) -> "Synth:method8"))
        .synthesize()
        .castTo(Base.class);

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
            asString("method4").equalTo("Base:method4").$(),
            asString("method6").equalTo("Synth:method6").$(),
            asString("method7").equalTo("Synth:method7").$(),
            asString("method8").equalTo("Synth:method8").$()
        )
    );
  }
}
