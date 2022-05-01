package com.github.dakusui.osynth.neo;

import com.github.dakusui.osynth.neo.BuiltInHandlerFactory.ForDescriptor;
import com.github.dakusui.osynth.neo.BuiltInHandlerFactory.ForEquals;
import com.github.dakusui.osynth.neo.BuiltInHandlerFactory.ForHashCode;
import com.github.dakusui.osynth.neo.BuiltInHandlerFactory.ForToString;

import java.util.*;

import static com.github.dakusui.pcond.Preconditions.requireNonNull;

public interface SynthesizedObject {
  @BuiltInHandlerFactory(ForDescriptor.class)
  @ReservedByOSynth
  Descriptor descriptor();

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

  @BuiltInHandlerFactory(ForEquals.class)
  @Override
  boolean equals(Object object);

  @BuiltInHandlerFactory(ForHashCode.class)
  @Override
  int hashCode();

  @BuiltInHandlerFactory(ForToString.class)
  @Override
  String toString();

  final class Descriptor {
    final List<Class<?>>                          interfaces;
    final Object                                  fallbackObject;
    final HashMap<MethodSignature, MethodHandler> methodHandlers;
    final ClassLoader                             classLoader;

    public Descriptor(List<Class<?>> interfaces, Map<MethodSignature, MethodHandler> methodHandlers, Object fallbackObject, ClassLoader classLoader) {
      this.methodHandlers = new HashMap<>(requireNonNull(methodHandlers));
      this.interfaces = new LinkedList<>(requireNonNull(interfaces));
      this.fallbackObject = fallbackObject;
      this.classLoader = classLoader;
    }

    public List<Class<?>> interfaces() {
      return this.interfaces;
    }

    public Object fallbackObject() {
      return this.fallbackObject;
    }

    public ClassLoader classLoader() {
      return this.classLoader;
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
