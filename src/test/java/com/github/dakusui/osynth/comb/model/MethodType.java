package com.github.dakusui.osynth.comb.model;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.comb.def.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static com.github.dakusui.osynth.Utils.rethrow;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public enum MethodType {
  NORMAL {
    @Override
    public BiFunction<Object, Object[], Object> createMethodHandler(Class<?>[] argTypes, ExceptionType exceptionType) {
      return (self, args) -> format("apply%s(%s) on methodHandler", argTypes.length, Arrays.toString(argTypes));
    }

    @Override
    public Class<?>[] interfaces(ExceptionType exceptionType) {
      return new Class[] { I1N.class, I2N.class };
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
    public BiFunction<Object, Object[], Object> createMethodHandler(Class<?>[] argTypes, ExceptionType exceptionType) {
      return (self, args) -> {
        try {
          return exceptionType.create(format("apply%s(%s) on methodHandler", argTypes.length, Arrays.toString(argTypes)));
        } catch (Throwable throwable) {
          throw new RuntimeException(throwable);
        }
      };
    }

    @Override
    public Class<?>[] interfaces(ExceptionType exceptionType) {
      return new Class[] { I1E.class, I2E.class };
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

  public abstract BiFunction<Object, Object[], Object> createMethodHandler(Class<?>[] argTypes, ExceptionType exceptionType);

  public abstract Class<?>[] interfaces(ExceptionType exceptionType);

  public abstract ObjectSynthesizer.FallbackHandlerFactory createFallbackHandlerFactory(ExceptionType exceptionType);

  public abstract List<?> handlerObjects(ExceptionType exceptionType);
}
