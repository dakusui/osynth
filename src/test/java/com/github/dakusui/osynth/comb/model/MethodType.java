package com.github.dakusui.osynth.comb.model;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.comb.def.I1;
import com.github.dakusui.osynth.comb.def.I1N;
import com.github.dakusui.osynth.comb.def.I2;
import com.github.dakusui.osynth.comb.def.I2N;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static com.github.dakusui.osynth.Utils.rethrow;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public enum MethodType {
  NORMAL {
    Map<List<Class<?>>, Map<ExceptionType, BiFunction<Object, Object[], Object>>> methodHandlerPool = new HashMap<>();
    Map<ExceptionType, List<?>> handlerObjectPool = new HashMap<>();
    Map<ExceptionType, ObjectSynthesizer.FallbackHandlerFactory> fallbackHandlerFactoryPool = new HashMap<>();

    @Override
    public BiFunction<Object, Object[], Object> createMethodHandler(Class<?>[] argTypes, ExceptionType exceptionType) {
      return updateEntryIfAbsent(methodHandlerPool, argTypes, exceptionType, (self, args) -> format("apply%s(%s) on methodHandler", argTypes.length, Arrays.toString(argTypes)));
    }

    @Override
    public Class<?>[] interfaces(ExceptionType exceptionType) {
      return new Class[] { I1N.class, I2N.class };
    }

    @Override
    public ObjectSynthesizer.FallbackHandlerFactory createFallbackHandlerFactory(ExceptionType exceptionType) {
      fallbackHandlerFactoryPool.computeIfAbsent(
          exceptionType,
          e -> proxyDescriptor -> method -> (o, objects) ->
              String.format("%s(%s) on FallbackHandler", method.getName(), Arrays.toString(method.getParameterTypes())));
      return fallbackHandlerFactoryPool.get(exceptionType);
    }

    @Override
    public List<?> handlerObjects(ExceptionType exceptionType) {
      handlerObjectPool.computeIfAbsent(exceptionType, e -> {
        I2 i2 = new I2() {
          @Override
          public String apply0_1() {
            throw new UnsupportedOperationException();
          }

          @Override
          public String implementorName() {
            return "I[2](handlerObject)";
          }
        };
        I1 i1 = new I1() {
          @Override
          public String apply0_2() {
            return i2.apply0_2();
          }

          @Override
          public String implementorName() {
            return "I[1](handlerObject)";
          }
        };
        return asList(
            i1,
            i2);
      });
      return handlerObjectPool.get(exceptionType);
    }
  },
  EXCEPTION {
    Map<List<Class<?>>, Map<ExceptionType, BiFunction<Object, Object[], Object>>> methodHandlerPool = new HashMap<>();
    Map<ExceptionType, List<?>> handlerObjectPool = new HashMap<>();
    Map<ExceptionType, ObjectSynthesizer.FallbackHandlerFactory> fallbackHandlerFactoryPool = new HashMap<>();

    @Override
    public BiFunction<Object, Object[], Object> createMethodHandler(Class<?>[] argTypes, ExceptionType exceptionType) {
      return updateEntryIfAbsent(methodHandlerPool, argTypes, exceptionType, (self, args) -> {
        try {
          return exceptionType.createException(format("apply%s(%s) on methodHandler", argTypes.length, Arrays.toString(argTypes)));
        } catch (Throwable throwable) {
          throw new RuntimeException(throwable);
        }
      });
    }

    @Override
    public Class<?>[] interfaces(ExceptionType exceptionType) {
      return exceptionType.createInterfaces();
    }

    @Override
    public ObjectSynthesizer.FallbackHandlerFactory createFallbackHandlerFactory(ExceptionType exceptionType) {
      fallbackHandlerFactoryPool.computeIfAbsent(exceptionType, e -> proxyDescriptor -> method -> (o, objects) -> {
        try {
          throw e.createException(String.format("%s(%s) on FallbackHandler", method.getName(), Arrays.toString(method.getParameterTypes())));
        } catch (Throwable throwable) {
          throw rethrow(throwable);
        }
      });
      return fallbackHandlerFactoryPool.get(exceptionType);
    }

    @Override
    public List<?> handlerObjects(ExceptionType exceptionType) {
      handlerObjectPool.computeIfAbsent(exceptionType, e -> {
        I2 i2 = new I2() {
          @Override
          public String apply0_1() {
            throw new UnsupportedOperationException();
          }

          @Override
          public String implementorName() throws Throwable {
            throw exceptionType.createException("I[2](handlerObject)");
          }
        };
        I1 i1 = new I1() {
          @Override
          public String apply0_2() {
            return i2.apply0_2();
          }

          @Override
          public String implementorName() throws Throwable {
            throw exceptionType.createException("I[1](handlerObject)");
          }
        };

        return asList(
            i1,
            i2);
      });
      return handlerObjectPool.get(exceptionType);
    }

  };

  public abstract BiFunction<Object, Object[], Object> createMethodHandler(Class<?>[] argTypes, ExceptionType exceptionType);

  public abstract Class<?>[] interfaces(ExceptionType exceptionType);

  public abstract ObjectSynthesizer.FallbackHandlerFactory createFallbackHandlerFactory(ExceptionType exceptionType);

  public abstract List<?> handlerObjects(ExceptionType exceptionType);

  BiFunction<Object, Object[], Object> updateEntryIfAbsent(Map<List<Class<?>>, Map<ExceptionType, BiFunction<Object, Object[], Object>>> pool, Class<?>[] argTypes, ExceptionType exceptionType, BiFunction<Object, Object[], Object> function) {
    if (!pool.containsKey(asList(argTypes)))
      pool.put(asList(argTypes), new HashMap<>());
    if (!pool.get(asList(argTypes)).containsKey(exceptionType))
      pool.get(asList(argTypes)).put(exceptionType, function);
    return pool.get(asList(argTypes)).get(exceptionType);
  }
}
