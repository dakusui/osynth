package com.github.dakusui.osynth.comb.model;

import com.github.dakusui.osynth.comb.def.I1;
import com.github.dakusui.osynth.comb.def.I1N;
import com.github.dakusui.osynth.comb.def.I2;
import com.github.dakusui.osynth.comb.def.I2N;
import com.github.dakusui.osynth2.core.MethodHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static java.lang.String.format;
import static java.util.Arrays.asList;

public enum MethodType {
  NORMAL {
    final Map<List<Class<?>>, Map<ExceptionType, MethodHandler>> methodHandlerPool = new HashMap<>();
    final Map<ExceptionType, List<?>> handlerObjectPool = new HashMap<>();

    @Override
    public MethodHandler createMethodHandler(Class<?>[] argTypes, ExceptionType exceptionType) {
      return updateEntryIfAbsent(methodHandlerPool, argTypes, exceptionType, (self, args) -> format("apply%s(%s) on methodHandler", argTypes.length, Arrays.toString(argTypes)));
    }

    @Override
    public Class<?>[] interfaces(ExceptionType exceptionType) {
      return new Class[] { I1N.class, I2N.class };
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
    final Map<List<Class<?>>, Map<ExceptionType, MethodHandler>> methodHandlerPool = new HashMap<>();
    final Map<ExceptionType, List<?>> handlerObjectPool = new HashMap<>();

    @Override
    public MethodHandler createMethodHandler(Class<?>[] argTypes, ExceptionType exceptionType) {
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

  public abstract MethodHandler createMethodHandler(Class<?>[] argTypes, ExceptionType exceptionType);

  public abstract Class<?>[] interfaces(ExceptionType exceptionType);

  public abstract List<?> handlerObjects(ExceptionType exceptionType);

  MethodHandler updateEntryIfAbsent(Map<List<Class<?>>, Map<ExceptionType, MethodHandler>> pool, Class<?>[] argTypes, ExceptionType exceptionType, MethodHandler function) {
    if (!pool.containsKey(asList(argTypes)))
      pool.put(asList(argTypes), new HashMap<>());
    if (!pool.get(asList(argTypes)).containsKey(exceptionType))
      pool.get(asList(argTypes)).put(exceptionType, function);
    return pool.get(asList(argTypes)).get(exceptionType);
  }
}
