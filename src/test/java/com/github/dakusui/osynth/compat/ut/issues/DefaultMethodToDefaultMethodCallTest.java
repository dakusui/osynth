package com.github.dakusui.osynth.compat.ut.issues;

import com.github.dakusui.osynth.compat.testwrappers.LegacyObjectSynthesizer;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static com.github.dakusui.crest.Crest.asInteger;
import static com.github.dakusui.crest.Crest.assertThat;
import static com.github.dakusui.osynth.compat.testwrappers.LegacyObjectSynthesizer.methodCall;

@RunWith(Enclosed.class)
public class DefaultMethodToDefaultMethodCallTest {
  public interface TestInterface {
    default int caller() {
      return callee();
    }

    default int callee() {
      return 1;
    }
  }

  @SuppressWarnings("NewClassNamingConvention")
  public static class GivenHandlerObjectImplementingTestInterfaceAndMethodHandlerForCalleeMethod extends UtBase {
    /**
     * This is a test for counter intuitive behavior.
     * Since implementation of a default method is "synthesized" in an implementation class when the implementing class is compiled,
     * call on the callee method from the caller is invoked directly on "this", not through the dynamic proxy.
     * Hence the method handler is ignored even if it is given to the object synthesize for the callee method.
     * <p>
     * Best practice to avoid this is not to add a handler object.
     */
    @Test
    public void whenCallerMethodIsInvoked$thenCalleeInHandlerObjectIsInvoked() throws NoSuchMethodException {
      TestInterface handlerObject = new TestInterface() {
        @Override
        public int callee() {
          return -100;
        }
      };
      TestInterface i = LegacyObjectSynthesizer.create(false)
          .handle(methodCall("callee").with((self, args) -> 100))
          .addInterface(TestInterface.class)
          .fallbackTo(handlerObject)
          .synthesize()
          .castTo(TestInterface.class);
      int result = i.caller();
      System.out.println(result);
      // This behavior is counter-intuitive.
      System.out.println(i.getClass().getMethod("callee").isSynthetic());
      System.out.println(handlerObject.getClass());
      System.out.println(handlerObject.getClass().getMethod("caller"));
      System.out.println(handlerObject.getClass().getMethod("caller").isSynthetic());
      System.out.println(handlerObject.getClass().getMethod("caller").isDefault());
      assertThat(result, asInteger().equalTo(100).$());
    }

    @Test
    public void whenCalleeMethodIsInvoked$thenCalleeInBaseInterfaceIsInvoked() {
      TestInterface i = LegacyObjectSynthesizer.create(false)
          .fallbackTo(new TestInterface() {
            @Override
            public int caller() {
              return -100;
            }
          })
          .handle(methodCall("caller").with((self, args) -> 100))
          .addInterface(TestInterface.class)
          .synthesize()
          .castTo(TestInterface.class);
      System.out.println(i.caller());
    }
  }

  @SuppressWarnings("NewClassNamingConvention")
  public static class GivenFallbackObjectOverridingCalleeMethod extends UtBase {
    @Test
    public void whenCallerMethodIsInvoked$thenCalleeInInterfaceDefaultIsRun() {
      TestInterface i = LegacyObjectSynthesizer.create(false)
          .addInterface(TestInterface.class)
          .fallbackTo(new Object() {
            /**
             * Even a compiler doesn't detect its usage, this method is called through the ObjectSynthesizer.
             * @return -99
             */
            @SuppressWarnings("unused")
            public int callee() {
              return -99;
            }
          })
          .synthesize()
          .castTo(TestInterface.class);
      int result = i.caller();
      System.out.println(result);
      assertThat(result, asInteger().equalTo(1).$());
    }
  }

  @SuppressWarnings("NewClassNamingConvention")
  public static class GivenNoHandlerObjectAndMethodHandlerForCalleeMethod extends UtBase {
    @Test
    public void whenCallerMethodIsInvoked$thenCalleeInBaseInterfaceIsInvoked() throws NoSuchMethodException {
      TestInterface i = LegacyObjectSynthesizer.create(false)
          .handle(methodCall("callee").with((self, args) -> 100))
          .addInterface(TestInterface.class)
          .synthesize()
          .castTo(TestInterface.class);
      int result = i.caller();
      System.out.println(result);
      // This behavior is counter-intuitive.
      System.out.println(i.getClass().getMethod("callee").isSynthetic());
      assertThat(result, asInteger().equalTo(100).$());
    }

    @Test
    public void whenCalleeMethodIsInvoked$thenCalleeInBaseInterfaceIsInvoked() {
      TestInterface i = LegacyObjectSynthesizer.create(false)
          .handle(methodCall("caller").with((self, args) -> 100))
          .addInterface(TestInterface.class)
          .synthesize()
          .castTo(TestInterface.class);
      System.out.println(i.callee());
    }
  }
}