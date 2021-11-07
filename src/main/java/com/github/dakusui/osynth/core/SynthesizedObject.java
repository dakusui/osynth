package com.github.dakusui.osynth.core;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.annotations.OsynthInternal;
import com.github.dakusui.osynth.utils.InternalUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static com.github.dakusui.osynth.utils.InternalUtils.invokeMethod;
import static com.github.dakusui.osynth.utils.InternalUtils.toMethodHandle;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.Preconditions.requireNonNull;
import static com.github.dakusui.pcond.functions.Predicates.isNotNull;
import static java.util.Collections.emptyList;

public interface SynthesizedObject {
  @OsynthInternal
  Object handleMethodInvocationRequest(Method method, Object[] args);

  @OsynthInternal
  Descriptor descriptor();

  @OsynthInternal
  <T> T castTo(Class<T> klass);

  @OsynthInternal
  Iterable<Class<?>> interfaces();

  @OsynthInternal
  Stream<MethodHandlerEntry> methodHandlersFor(Method method);

  @OsynthInternal
  SynthesizedObject base();

  @OsynthInternal
  Object fallback();

  interface Descriptor {
    default ClassLoader classLoader() {
      return this.getClass().getClassLoader();
    }

    List<MethodHandlerEntry> registeredMethodHandlers();

    List<Class<?>> registeredInterfaceClasses();

    Optional<Object> fallbackObject();

    class Builder {
      final List<MethodHandlerEntry> methodHandlerEntries = new LinkedList<>();
      final List<Class<?>>           interfaceClasses     = new LinkedList<>();
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
      public Builder addMethodHandler(MethodHandlerEntry methodHandlerEntry) {
        this.methodHandlerEntries.add(methodHandlerEntry);
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
        List<MethodHandlerEntry> methodHandlerEntries = new ArrayList<>(this.methodHandlerEntries);
        List<Class<?>> interfaceClasses = new ArrayList<>(new LinkedHashSet<>(this.interfaceClasses));
        Object fallbackObject = this.fallbackObject;
        return new Descriptor() {
          @Override
          public List<MethodHandlerEntry> registeredMethodHandlers() {
            return methodHandlerEntries;
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
    private final Object proxy;

    public Impl(Object proxy, Descriptor descriptor) {
      assert that(descriptor, isNotNull());
      this.proxy = proxy;
      this.descriptor = descriptor;
    }


    @Override
    @OsynthInternal
    public Descriptor descriptor() {
      return this.descriptor;
    }

    @OsynthInternal
    public Object handleMethodInvocationRequest(Method method, Object[] args) {
      if (method.getAnnotation(OsynthInternal.class) != null)
        return invokeMethod(method, this, args);
      return this.methodHandlerFor(method)
          .map(MethodHandlerEntry::function)
          .orElseGet(() -> methodHandlerBasedOnDefaultImplementationFor(method)
              .orElseGet(() -> methodHandlerFunctionInFallbackObjectFor(method)))
          .apply(this, args);
    }

    @OsynthInternal
    public MethodHandler methodHandlerFunctionInFallbackObjectFor(Method method) {
      return InternalUtils.createMethodHandlerFor(descriptor().fallbackObject().orElse(null), method);
    }

    @OsynthInternal
    public Optional<MethodHandler> methodHandlerBasedOnDefaultImplementationFor(Method method) {
      return toMethodHandle(method).map(InternalUtils::toMethodHandler);
    }

    @Override
    @OsynthInternal
    public <T> T castTo(Class<T> klass) {
      return klass.cast(this.proxy);
      /*
      return require(klass, and(
          isNotNull(),
          isInterfaceClass(),
          matchesAnyOf(
              descriptor().registeredInterfaceClasses(),
              isAssignableFrom(klass)))).cast(this);
       */
    }

    @Override
    @OsynthInternal
    public Iterable<Class<?>> interfaces() {
      return descriptor().registeredInterfaceClasses();
    }

    @OsynthInternal
    public Optional<MethodHandlerEntry> methodHandlerFor(Method method) {
      return methodHandlersFor(method).findFirst();
    }

    @Override
    @OsynthInternal
    public Stream<MethodHandlerEntry> methodHandlersFor(Method method) {
      return descriptor().registeredMethodHandlers().stream()
          .filter(each -> each.matcher().test(method));
    }

    @Override
    @OsynthInternal
    public SynthesizedObject base() {
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
        public List<MethodHandlerEntry> registeredMethodHandlers() {
          return emptyList();
        }
      });
    }

    @Override
    @OsynthInternal
    public Object fallback() {
      return descriptor().fallbackObject().orElse(null);
    }
  }
}