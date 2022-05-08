package com.github.dakusui.osynth.compat.perf;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.compat.utils.UtBase;
import org.junit.Test;

import static com.github.dakusui.osynth.compat.testwrappers.LegacyObjectSynthesizer.methodCall;

@SuppressWarnings("NewClassNamingConvention")
public class Perf extends UtBase {
  interface Acc {
    Integer increment(Integer value);
  }

  interface AccDefault extends Acc {
    default Integer increment(Integer value) {
      return value + 1;
    }
  }

  /**
   * .ExactSignatureMode (session 1)
   * ----
   * 0:6.154375[msec]
   * 1:4.585292[msec]
   * 2:3.806083[msec]
   * 3:3.816167[msec]
   * 4:3.785917[msec]
   * 5:3.812958[msec]
   * 6:3.965875[msec]
   * 7:4.189083[msec]
   * 8:6.371250[msec]
   * 9:3.865625[msec]
   * numExecutions:1000000
   * total:44.479375[msec]
   * result:1000001
   * ----
   *
   * .SignatureMatchingMode (session 2)
   * ----
   * 0:5.7000[msec]
   * 1:4.502083[msec]
   * 2:3.721417[msec]
   * 3:3.696833[msec]
   * 4:3.620084[msec]
   * 5:3.716875[msec]
   * 6:4.357791[msec]
   * 7:3.919667[msec]
   * 8:4.206292[msec]
   * 9:3.815166[msec]
   * numExecutions:1000000
   * total:40.695125[msec]
   * result:1000001
   * ----
   *
   * .SignatureMatchingMode (with simple hashmap)
   * ----
   * 0:4.579209[msec]
   * 1:3.901500[msec]
   * 2:3.42500[msec]
   * 3:3.172666[msec]
   * 4:3.45292[msec]
   * 5:3.56708[msec]
   * 6:3.58250[msec]
   * 7:3.58459[msec]
   * 8:4.304791[msec]
   * 9:3.388792[msec]
   * numExecutions:1000000
   * total:34.829834[msec]
   * result:1000001
   * ----
   *
   * .Simplified Method Matching mechanism. Showed slight performance degradation.
   * ----
   * commit 4e455cb480d7cfa7d2a2903db3db2a8fec86b52b (HEAD -> master-refining)
   * Author: Hiroshi Ukai <dakusui@gmail.com>
   * Date:   Sun May 8 13:55:17 2022 +0900
   *
   *     Simplify matching structure.
   *
   * 0:5.317750[msec]
   * 1:4.764667[msec]
   * 2:3.787500[msec]
   * 3:3.734625[msec]
   * 4:3.759875[msec]
   * 5:3.763875[msec]
   * 6:3.753541[msec]
   * 7:3.850250[msec]
   * 8:6.901375[msec]
   * 9:3.811042[msec]
   * numExecutions:1000000
   * total:43.533458[msec]
   * result:1000001
   * ----
   */
  @Test
  public void performWithMethodHandler() {
    perform(synthesizeObjectForMethodHandler());
  }

  /**
   * .ExactSignatureMode (session 1)
   * ----
   * 0:198.692708[msec]
   * 1:132.293125[msec]
   * 2:128.832000[msec]
   * 3:126.8333[msec]
   * 4:127.588709[msec]
   * 5:125.820208[msec]
   * 6:127.637292[msec]
   * 7:127.440166[msec]
   * 8:129.62084[msec]
   * 9:129.373416[msec]
   * numExecutions:1000000
   * total:1352.831916[msec]
   * result:1000001
   * ----
   *
   * .SignatureMatchingMode (session 2)
   * ----
   * 0:264.717000[msec]
   * 1:171.849583[msec]
   * 2:164.32250[msec]
   * 3:161.49708[msec]
   * 4:162.53000[msec]
   * 5:163.491792[msec]
   * 6:160.726000[msec]
   * 7:161.231458[msec]
   * 8:144.532959[msec]
   * 9:148.686541[msec]
   * numExecutions:1000000
   * total:1702.433750[msec]
   * result:1000001
   * ----
   */
  @Test
  public void performWithFallbackObject() {
    perform(synthesizeObjectForFallbackObject());
  }

  /**
   * .ExactSignatureMode (session 1)
   * ----
   * 0:512.844791[msec]
   * 1:62.209417[msec]
   * 2:54.796292[msec]
   * 3:44.247541[msec]
   * 4:31.484000[msec]
   * 5:51.365875[msec]
   * 6:55.628917[msec]
   * 7:31.814667[msec]
   * 8:34.651041[msec]
   * 9:38.824709[msec]
   * numExecutions:1000000
   * total:918.504375[msec]
   * result:1000001
   * ----
   *
   * .SignatureMatchingMode (session 2)
   * ----
   * 0:461.701292[msec]
   * 1:131.117333[msec]
   * 2:83.764875[msec]
   * 3:47.198167[msec]
   * 4:40.849333[msec]
   * 5:56.418417[msec]
   * 6:70.484667[msec]
   * 7:36.240458[msec]
   * 8:38.889375[msec]
   * 9:48.761875[msec]
   * numExecutions:1000000
   * total:1016.121750[msec]
   * result:1000001
   * ----
   */
  @Test
  public void performWithDefaultMethod() {
    perform(synthesizeObjectForDefaultMethod());
  }

  /**
   * .Session 1
   * ----
   * 0:4.753833[msec]
   * 1:3.717667[msec]
   * 2:3.252250[msec]
   * 3:2.328041[msec]
   * 4:1.341417[msec]
   * 5:2.406167[msec]
   * 6:4.264625[msec]
   * 7:0.734791[msec]
   * 8:0.361750[msec]
   * 9:0.327959[msec]
   * numExecutions:1000000
   * total:23.501708[msec]
   * result:1000001
   * ----
   *
   * .Session 2
   * ----
   * 0:5.157125[msec]
   * 1:2.829375[msec]
   * 2:2.957500[msec]
   * 3:1.123792[msec]
   * 4:1.74416[msec]
   * 5:1.71250[msec]
   * 6:1.69125[msec]
   * 7:1.72042[msec]
   * 8:1.74250[msec]
   * 9:1.512708[msec]
   * numExecutions:1000000
   * total:19.57750[msec]
   * result:1000001
   * ----
   */
  @Test
  public void performWithPlainObject() {
    perform(createObject());
  }

  public void perform(Acc acc) {
    long before = System.nanoTime();
    long p = before;
    int session = 0;
    Integer i;
    int numExecutions = blockSize() * numSessions();
    for (i = 0; i <= numExecutions; i = acc.increment(i)) {
      if (i % blockSize() == 0 && i > 0) {
        long now = System.nanoTime();
        printTime(session, (now - p));
        p = now;
        session++;
      }
    }
    long time = System.nanoTime() - before;
    System.out.println("numExecutions:" + numExecutions);
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
    System.out.println(label + ":" + (time / 1_000_000) + "." + (time % 1_000_000) + "[msec]");
  }

  Acc createObject() {
    return value -> value + 1;
  }

  Acc synthesizeObjectForMethodHandler() {
    return new ObjectSynthesizer()
        .addInterface(Acc.class)
        .handle(methodCall("increment", Integer.class).with((self, args) -> ((Integer) args[0]) + 1))
        .synthesize()
        .castTo(Acc.class);
  }

  Acc synthesizeObjectForFallbackObject() {
    return new ObjectSynthesizer()
        .addInterface(Acc.class)
        .fallbackTo((Acc) value -> value + 1)
        .synthesize()
        .castTo(Acc.class);
  }

  Acc synthesizeObjectForDefaultMethod() {
    return new ObjectSynthesizer()
        .addInterface(AccDefault.class)
        .synthesize()
        .castTo(Acc.class);
  }
}
