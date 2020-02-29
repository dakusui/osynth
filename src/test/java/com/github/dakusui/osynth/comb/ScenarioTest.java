package com.github.dakusui.osynth.comb;

import com.github.dakusui.crest.Crest;
import com.github.dakusui.jcunit8.factorspace.Parameter;
import com.github.dakusui.jcunit8.runners.junit4.JCUnit8;
import com.github.dakusui.jcunit8.runners.junit4.annotations.Condition;
import com.github.dakusui.jcunit8.runners.junit4.annotations.From;
import com.github.dakusui.jcunit8.runners.junit4.annotations.ParameterSource;
import com.github.dakusui.osynth.MethodHandler;
import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.comb.def.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static com.github.dakusui.crest.Crest.*;
import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.osynth.Utils.rethrow;
import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * I1,
 * I2,
 * H1,
 * H2,
 * F1
 * m(p1,p2)
 * <p>
 * I: 0, 1, 2    ... registered interfaces
 * H: 0, 1, 2    ... registered method handlers
 * F: 0, 1       ... registered fallback
 * Ma: 0, 1, 2   ... num args of the reacting method
 * Me: 0, 1      ... thrown exception or not
 */
@RunWith(JCUnit8.class)
public class ScenarioTest {
  public enum MethodType {
    NORMAL {
      @Override
      public BiFunction<Object, Object[], Object> createMethodHandler(int numArgs, Class<?>[] argTypes, ExceptionType exceptionType) {
        return (self, args) -> format("apply%s(%s) on methodHandler", numArgs, Arrays.toString(argTypes));
      }

      @Override
      public Class<?>[] interfaces(ExceptionType exceptionType) {
        return new Class[]{I1N.class, I2N.class};
      }

      @Override
      public ObjectSynthesizer.FallbackHandlerFactory createFallbackHandlerFactory(ExceptionType exceptionType) {
        return proxyDescriptor -> method -> (o, objects) -> String.format("%s(%s) on FallbackHandler", method.getName(), Arrays.toString(method.getParameterTypes()));
      }

      @Override
      public List<?> handlerObjects(ExceptionType exceptionType) {
        return asList(
            (I) () -> "I[1](handlerObject)",
            () -> "I[2](handlerObject)");
      }
    },
    EXCEPTION {
      @Override
      public BiFunction<Object, Object[], Object> createMethodHandler(int numArgs, Class<?>[] argTypes, ExceptionType exceptionType) {
        return (self, args) -> exceptionType.create(format("apply%s(%s) on methodHandler", numArgs, Arrays.toString(argTypes)));
      }

      @Override
      public Class<?>[] interfaces(ExceptionType exceptionType) {
        return new Class[]{I1E.class, I2E.class};
      }

      @Override
      public ObjectSynthesizer.FallbackHandlerFactory createFallbackHandlerFactory(ExceptionType exceptionType) {
        return proxyDescriptor -> method -> (o, objects) -> {
          try {
            throw exceptionType.create(String.format("%s(%s) on FallbackHandler", method.getName(), Arrays.toString(method.getParameterTypes())));
          } catch (Throwable throwable) {
            throw rethrow(throwable);
          }
        };
      }

      @Override
      public List<?> handlerObjects(ExceptionType exceptionType) {
        return asList(
            (I) () -> {
              throw exceptionType.create("I[1](handlerObject)");
            },
            () -> {
              throw exceptionType.create("I[2](handlerObject)");
            });
      }
    };

    public abstract BiFunction<Object, Object[], Object> createMethodHandler(int numArgs, Class<?>[] argTypes, ExceptionType exceptionType);

    public abstract Class<?>[] interfaces(ExceptionType exceptionType);

    public abstract ObjectSynthesizer.FallbackHandlerFactory createFallbackHandlerFactory(ExceptionType exceptionType);

    public abstract List<?> handlerObjects(ExceptionType exceptionType);
  }

  public enum ExceptionType {
    RUNTIME_EXCEPTION {
      @Override
      Throwable create(String meesage) {
        throw new RuntimeException(meesage);
      }
    },
    ERROR {
      @Override
      Throwable create(String message) {
        throw new Error(message);
      }
    },
    CHEKED_EXCEPTION {
      @Override
      Throwable create(String message) {
        return new IOException(message);
      }
    },
    NONE {
      @Override
      Throwable create(String message) {
        return null;
      }
    };

    abstract Throwable create(String message);
  }

  @ParameterSource
  public Parameter.Factory<Boolean> auto() {
    return Parameter.Simple.Factory.of(asList(false, true));
  }

  @ParameterSource
  public Parameter.Factory<Integer> numInterfaces() {
    return Parameter.Simple.Factory.of(asList(1, 2, 0));
  }

  @ParameterSource
  public Parameter.Factory<Integer> numHandlerObjects() {
    return Parameter.Simple.Factory.of(asList(1, 2, 0));
  }

  @ParameterSource
  public Parameter.Factory<Boolean> customFallback() {
    return Parameter.Simple.Factory.of(asList(true, false));
  }

  @ParameterSource
  public Parameter.Factory<Integer> numArgs() {
    return Parameter.Simple.Factory.of(asList(1, 2, 0));
  }

  @ParameterSource
  public Parameter.Factory<MethodType> methodType() {
    return Parameter.Simple.Factory.of(asList(MethodType.NORMAL, MethodType.EXCEPTION));
  }

  @ParameterSource
  public Parameter.Factory<ExceptionType> exceptionType() {
    return Parameter.Simple.Factory.of(asList(ExceptionType.NONE, ExceptionType.CHEKED_EXCEPTION, ExceptionType.RUNTIME_EXCEPTION, ExceptionType.ERROR));
  }

  @Condition(constraint = true)
  public boolean cons(@From("methodType") MethodType methodType, @From("exceptionType") ExceptionType exceptionType) {
    if (methodType == MethodType.NORMAL)
      return exceptionType == ExceptionType.NONE;
    return true;
  }

  //  @Ignore
//  @Test
  public void example() {
    Object obj = new ObjectSynthesizer()
        .handle(createMethodHandler(1, MethodType.NORMAL, null))
        .addHandlerObject(new Object())
        .addInterface(I1N.class)
        .addInterface(I2N.class)
        .fallbackHandlerFactory(createFallbackHandlerFactory(MethodType.NORMAL, null))
        .synthesize();
    Crest.assertThat(
        obj,
        asString("apply1", 100).equalTo("apply1").$()
    );
  }

  @Test
  public void test2(@From("auto") boolean auto,
                    @From("numInterfaces") int numInterfaces,
                    @From("numHandlerObjects") int numHandlerObjects,
                    @From("customFallback") boolean customFallback,
                    @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
                    @From("exceptionType") ExceptionType exceptionType
  ) {
    ObjectSynthesizer objectSynthesizer = ObjectSynthesizer.create(auto);
    Object obj = new ObjectSynthesizerWrapper(objectSynthesizer)
        .addMethodHandlers(methodType, numArgs, exceptionType)
        .addHandlerObjects(methodType, exceptionType, numHandlerObjects)
        .addInterfaces(methodType, exceptionType, numInterfaces)
        .setFallbackHandlerFactory(methodType, exceptionType, customFallback)
        .synthesize();
    Crest.assertThat(
        obj,
        allOf(
            asString("apply1", 100).containsString("apply1").$(),
            asBoolean("equals", obj).isTrue().$(),
            asBoolean(call("equals", (Object) null).$()).isFalse().$()
        )
    );
  }

  private static ObjectSynthesizer.FallbackHandlerFactory createFallbackHandlerFactory(MethodType methodType, ExceptionType exceptionType) {
    return methodType.createFallbackHandlerFactory(exceptionType);
  }

  public static MethodHandler createMethodHandler(int numArgs, MethodType methodType, ExceptionType exceptionType) {
    Class<?>[] argTypes = new Class[numArgs];
    for (int i = 0; i < numArgs; i++)
      argTypes[i] = int.class;
    BiFunction<Object, Object[], Object> methodHandlingFunction;
    methodHandlingFunction = methodType.createMethodHandler(numArgs, argTypes, exceptionType);
    return methodCall(format("apply%s", numArgs), argTypes).with(methodHandlingFunction);
  }

  static class ObjectSynthesizerWrapper {
    final ObjectSynthesizer objectSynthesizer;

    ObjectSynthesizerWrapper(ObjectSynthesizer objectSynthesizer) {
      this.objectSynthesizer = objectSynthesizer;
    }

    private ObjectSynthesizerWrapper addInterfaces(MethodType methodType, ExceptionType exceptionType, int numInterfaces) {
      Class<?>[] interfaces = methodType.interfaces(exceptionType);
      for (int i = 0; i < numInterfaces; i++)
        objectSynthesizer.addInterface(interfaces[i]);
      return this;
    }

    public ObjectSynthesizerWrapper addHandlerObjects(MethodType methodType, ExceptionType exceptionType, int numHandlerObjects) {
      List<?> handlerObjects = methodType.handlerObjects(exceptionType);
      for (int i = 0; i < numHandlerObjects; i++)
        objectSynthesizer.addHandlerObject(handlerObjects.get(i));
      return this;
    }

    public ObjectSynthesizerWrapper setFallbackHandlerFactory(MethodType methodType, ExceptionType exceptionType, boolean customFallback) {
      if (customFallback)
        objectSynthesizer.fallbackHandlerFactory(createFallbackHandlerFactory(methodType, exceptionType));
      return this;
    }

    public ObjectSynthesizerWrapper addMethodHandlers(MethodType methodType, int numArgs, ExceptionType exceptionType) {
      return new ObjectSynthesizerWrapper(objectSynthesizer.handle(createMethodHandler(numArgs, methodType, exceptionType)));
    }

    public <T> T synthesize() {
      return this.objectSynthesizer.synthesize();
    }

  }
}
