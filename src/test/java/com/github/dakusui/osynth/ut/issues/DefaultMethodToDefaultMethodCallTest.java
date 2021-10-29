package com.github.dakusui.osynth.ut.issues;

import com.github.dakusui.osynth.compat.CompatObjectSynthesizer;
import com.github.dakusui.osynth.utils.UtBase;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static com.github.dakusui.crest.Crest.asInteger;
import static com.github.dakusui.crest.Crest.assertThat;
import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;

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
      TestInterface i = CompatObjectSynthesizer.create(false)
          .addHandlerObject(handlerObject)
          .handle(methodCall("callee").with((self, args) -> 100))
          .addInterface(TestInterface.class)
          .synthesize();
      int result = i.caller();
      System.out.println(result);
      // This behavior is counter-intuitive.
      System.out.println(i.getClass().getMethod("callee").isSynthetic());
      System.out.println(handlerObject.getClass());
      System.out.println(handlerObject.getClass().getMethod("caller"));
      System.out.println(handlerObject.getClass().getMethod("caller").isSynthetic());
      System.out.println(handlerObject.getClass().getMethod("caller").isDefault());
      assertThat(result, asInteger().equalTo(-100).$());
    }

    @Test
    public void whenCalleeMethodIsInvoked$thenCalleeInBaseInterfaceIsInvoked() {
      TestInterface i = CompatObjectSynthesizer.create(false)
          .addHandlerObject(new TestInterface() {
            @Override
            public int caller() {
              return -100;
            }
          })
          .handle(methodCall("caller").with((self, args) -> 100))
          .addInterface(TestInterface.class)
          .synthesize();
      System.out.println(i.caller());
    }
  }

  public static class GivenSimpleHandlerObjectOverridingCalleeMethod extends UtBase {
    @Test
    public void whenCallerMethodIsInvoked$thenCalleeInHandlerObjectIsRun() {
      TestInterface i = CompatObjectSynthesizer.create(false)
          .addHandlerObject(new Object() {
            /**
             * Even a compiler doesn't its usage, this method is called through the ObjectSynthesizer.
             * @return -99
             */
            @SuppressWarnings("unused")
            public int callee() {
              return -99;
            }
          })
          .addInterface(TestInterface.class)
          .synthesize();
      int result = i.caller();
      System.out.println(result);
      assertThat(result, asInteger().equalTo(-99).$());
    }
  }

  public static class GivenNoHandlerObjectAndMethodHandlerForCalleeMethod extends UtBase {
    @Test
    public void whenCallerMethodIsInvoked$thenCalleeInBaseInterfaceIsInvoked() throws NoSuchMethodException {
      TestInterface i = CompatObjectSynthesizer.create(false)
          .handle(methodCall("callee").with((self, args) -> 100))
          .addInterface(TestInterface.class)
          .synthesize();
      int result = i.caller();
      System.out.println(result);
      // This behavior is counter-intuitive.
      System.out.println(i.getClass().getMethod("callee").isSynthetic());
      assertThat(result, asInteger().equalTo(100).$());
    }

    @Test
    public void whenCalleeMethodIsInvoked$thenCalleeInBaseInterfaceIsInvoked() {
      TestInterface i = CompatObjectSynthesizer.create(false)
          .handle(methodCall("caller").with((self, args) -> 100))
          .addInterface(TestInterface.class)
          .synthesize();
      System.out.println(i.callee());
    }
  }
}