package com.github.dakusui.osynth.core;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.utils.InternalUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.github.dakusui.osynth.utils.InternalPredicates.*;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.Preconditions.require;
import static com.github.dakusui.pcond.Preconditions.requireNonNull;
import static com.github.dakusui.pcond.functions.Predicates.and;
import static com.github.dakusui.pcond.functions.Predicates.isNotNull;
import static java.util.Collections.emptyList;

public interface SynthesizedObject {

  default Object handleMethod(Method method, Object[] args) {
    return this.methodHandlerFor(method)
        .map(MethodHandler::function)
        .orElseGet(() -> methodHandlingFunctionBasedOnDefaultImplementationFor(method)
            .orElseGet(() -> methodHandlingFunctionInFallbackObjectFor(method)))
        .apply(this, args);
  }

  default BiFunction<SynthesizedObject, Object[], Object> methodHandlingFunctionInFallbackObjectFor(Method method) {
    return InternalUtils.createMethodHandlingFunctionFor(descriptor().fallbackObject().orElse(null), method);
  }

  default Optional<BiFunction<SynthesizedObject, Object[], Object>> methodHandlingFunctionBasedOnDefaultImplementationFor(Method method) {
    return InternalUtils.toMethodHandle(method).map(InternalUtils::toMethodHandlingFunction);
  }

  default <T> T castTo(Class<T> klass) {
    return require(klass, and(
        isNotNull(),
        isInterfaceClass(),
        matchesAnyOf(
            descriptor().registeredInterfaceClasses(),
            isAssignableFrom(klass)))).cast(this);
  }

  default Iterable<Class<?>> interfaces() {
    return descriptor().registeredInterfaceClasses();
  }

  default Optional<MethodHandler> methodHandlerFor(Method method) {
    return methodHandlersFor(method).findFirst();
  }

  default Stream<MethodHandler> methodHandlersFor(Method method) {
    return descriptor().registeredMethodHandlers().stream()
        .filter(each -> each.matcher().test(method));
  }

  default SynthesizedObject base() {
    Descriptor baseDescriptor = descriptor();
    return ObjectSynthesizer.createSynthesizedObject(new Descriptor() {
      @Override
      public List<Class<?>> registeredInterfaceClasses() {
        return baseDescriptor.registeredInterfaceClasses();
      }

      @Override
      public Optional<Object> fallbackObject() {
        return baseDescriptor.fallbackObject();
      }

      @Override
      public List<MethodHandler> registeredMethodHandlers() {
        return emptyList();
      }
    });
  }

  default Object fallback() {
    return descriptor().fallbackObject().orElse(null);
  }

  Descriptor descriptor();

  interface Descriptor {
    default ClassLoader classLoader() {
      return this.getClass().getClassLoader();
    }

    List<MethodHandler> registeredMethodHandlers();

    List<Class<?>> registeredInterfaceClasses();

    Optional<Object> fallbackObject();

    class Builder {
      final List<MethodHandler> methodHandlers   = new LinkedList<>();
      final List<Class<?>>      interfaceClasses = new LinkedList<>();
      Object      fallbackObject = null;
      ClassLoader classLoader;

      public Builder() {
        this.classLoader(Thread.currentThread().getContextClassLoader());
      }

      @SuppressWarnings("UnusedReturnValue")
      public Builder classLoader(ClassLoader classLoader) {
        this.classLoader = requireNonNull(classLoader);
        return this;
      }

      @SuppressWarnings("UnusedReturnValue")
      public Builder addMethodHandler(MethodHandler methodHandler) {
        this.methodHandlers.add(methodHandler);
        return this;
      }

      @SuppressWarnings("UnusedReturnValue")
      public Builder addInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClasses.add(interfaceClass);
        return this;
      }

      @SuppressWarnings("UnusedReturnValue")
      public Builder fallbackObject(Object fallbackObject) {
        this.fallbackObject = fallbackObject;
        return this;
      }

      public Descriptor build() {
        List<MethodHandler> methodHandlers = new ArrayList<>(this.methodHandlers);
        List<Class<?>> interfaceClasses = new ArrayList<>(new LinkedHashSet<>(this.interfaceClasses));
        Object fallbackObject = this.fallbackObject;
        return new Descriptor() {
          @Override
          public List<MethodHandler> registeredMethodHandlers() {
            return methodHandlers;
          }

          @Override
          public List<Class<?>> registeredInterfaceClasses() {
            return interfaceClasses;
          }

          @Override
          public Optional<Object> fallbackObject() {
            return Optional.ofNullable(fallbackObject);
          }
        };
      }
    }
  }

  class Impl implements SynthesizedObject {

    private final Descriptor descriptor;

    public Impl(Descriptor descriptor) {
      assert that(descriptor, isNotNull());
      this.descriptor = descriptor;
    }


    @Override
    public Descriptor descriptor() {
      return this.descriptor;
    }
  }
}