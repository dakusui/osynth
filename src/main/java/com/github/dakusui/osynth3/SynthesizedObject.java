package com.github.dakusui.osynth3;

import com.github.dakusui.osynth3.annotations.BuiltInHandlerFactory;
import com.github.dakusui.osynth3.annotations.ReservedByOSynth;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.github.dakusui.osynth3.SynthesizedObject.PrivateUtils.reservedMethodSignatures;
import static com.github.dakusui.osynth.utils.AssertionUtils.methodIsAnnotationPresent;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.Preconditions.requireNonNull;
import static com.github.dakusui.pcond.functions.Predicates.and;
import static com.github.dakusui.pcond.functions.Predicates.isNotNull;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toSet;

public interface SynthesizedObject {
  Set<MethodSignature> RESERVED_METHOD_SIGNATURES = reservedMethodSignatures();

  @BuiltInHandlerFactory(BuiltInHandlerFactory.ForDescriptor.class)
  @ReservedByOSynth
  Descriptor descriptor();

  @ReservedByOSynth
  default MethodHandler methodHandlerFor(MethodSignature methodSignature) {
    return descriptor().methodHandlers.get(methodSignature);
  }

  @ReservedByOSynth
  default List<Class<?>> interfaces() {
    return this.descriptor().interfaces();
  }

  @ReservedByOSynth
  default Object fallbackObject() {
    return descriptor().fallbackObject();
  }

  @ReservedByOSynth
  default <T> T castTo(Class<T> interfaceClass) {
    requireNonNull(interfaceClass);
    return interfaces().stream()
        .filter(interfaceClass::isAssignableFrom)
        .findFirst()
        .map(each -> interfaceClass.cast(this))
        .orElseThrow(NoSuchElementException::new);
  }

  @BuiltInHandlerFactory(BuiltInHandlerFactory.ForEquals.class)
  @Override
  boolean equals(Object object);

  @BuiltInHandlerFactory(BuiltInHandlerFactory.ForHashCode.class)
  @Override
  int hashCode();

  @BuiltInHandlerFactory(BuiltInHandlerFactory.ForToString.class)
  @Override
  String toString();

  enum PrivateUtils {
    ;

    public static Set<MethodSignature> reservedMethodSignatures() {
      return Arrays.stream(SynthesizedObject.class.getMethods())
          .filter(each -> each.isAnnotationPresent(ReservedByOSynth.class))
          .map(MethodSignature::create)
          .collect(toSet());
    }

    static Stream<MethodHandlerEntry> createMethodHandlersForBuiltInMethods(Descriptor descriptor, BiConsumer<MethodSignature, MethodHandler> updater) {
      return Arrays.stream(SynthesizedObject.class.getMethods())
          .filter(each -> each.isAnnotationPresent(BuiltInHandlerFactory.class))
          .map((Method eachMethod) -> MethodHandlerEntry.create(
              MethodSignature.create(eachMethod),
              createBuiltInMethodHandlerFor(eachMethod, descriptor)));
    }

    static MethodHandler createBuiltInMethodHandlerFor(Method method, Descriptor descriptor) {
      assert that(method, and(
          isNotNull(),
          methodIsAnnotationPresent(BuiltInHandlerFactory.class)));
      BuiltInHandlerFactory annotation = method.getAnnotation(BuiltInHandlerFactory.class);
      try {
        return annotation.value().newInstance().create(descriptor);
      } catch (InstantiationException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  final class Descriptor {
    final List<Class<?>>                          interfaces;
    final Object                                  fallbackObject;
    final HashMap<MethodSignature, MethodHandler> methodHandlers;
    final ClassLoader                             classLoader;

    public Descriptor(List<Class<?>> interfaces, Map<MethodSignature, MethodHandler> methodHandlers, Object fallbackObject, ClassLoader classLoader) {
      this.methodHandlers = new HashMap<>(Objects.requireNonNull(methodHandlers));
      this.interfaces = new LinkedList<>(Objects.requireNonNull(interfaces));
      this.fallbackObject = Objects.requireNonNull(fallbackObject);
      this.classLoader = Objects.requireNonNull(classLoader);
    }

    public List<Class<?>> interfaces() {
      return unmodifiableList(this.interfaces);
    }

    public Object fallbackObject() {
      return this.fallbackObject;
    }

    public ClassLoader classLoader() {
      return this.classLoader;
    }

    public Map<MethodSignature, MethodHandler> methodHandlers() {
      return unmodifiableMap(this.methodHandlers);
    }

    public static class Builder {
      final List<Class<?>>                          interfaces;
      final HashMap<MethodSignature, MethodHandler> methodHandlers;
      Object fallbackObject;
      private ClassLoader classLoader;

      public Builder() {
        interfaces = new LinkedList<>();
        methodHandlers = new HashMap<>();
      }

      public Builder(Descriptor descriptor) {
        this();
        this.interfaces.addAll(descriptor.interfaces());
        this.methodHandlers.putAll(descriptor.methodHandlers());
        this.classLoader = descriptor.classLoader;
        this.fallbackObject = descriptor.fallbackObject;
      }

      public Builder fallbackObject(Object fallbackObject) {
        this.fallbackObject = fallbackObject;
        return this;
      }

      public Builder classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
      }

      public Builder addInterface(Class<?> interfaceClass) {
        this.interfaces.add(interfaceClass);
        return this;
      }

      public Builder addMethodHandler(MethodSignature forMethod, MethodHandler methodHandler) {
        this.methodHandlers.put(forMethod, methodHandler);
        return this;
      }

      public Descriptor build() {
        return new Descriptor(this.interfaces, this.methodHandlers, this.fallbackObject, this.classLoader);
      }
    }
  }
}
