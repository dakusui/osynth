package com.github.dakusui.osynth2.core;

import com.github.dakusui.osynth2.annotations.BuiltInHandlerFactory;
import com.github.dakusui.osynth2.annotations.ReservedByOSynth;

import java.util.*;

import static com.github.dakusui.osynth2.core.SynthesizedObject.InternalUtils.reservedMethodSignatures;
import static com.github.dakusui.pcond.Preconditions.requireNonNull;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toSet;

public interface SynthesizedObject {
  Set<MethodSignature> RESERVED_METHOD_SIGNATURES = reservedMethodSignatures();

  @BuiltInHandlerFactory(BuiltInHandlerFactory.ForDescriptor.class)
  @ReservedByOSynth
  Descriptor descriptor();

  @ReservedByOSynth
  default <T> T castTo(Class<T> interfaceClass) {
    requireNonNull(interfaceClass);
    return descriptor().interfaces().stream()
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

  enum InternalUtils {
    ;

    static Set<MethodSignature> reservedMethodSignatures() {
      return Arrays.stream(SynthesizedObject.class.getMethods())
          .filter(each -> each.isAnnotationPresent(ReservedByOSynth.class))
          .map(MethodSignature::create)
          .collect(toSet());
    }

  }

  final class Descriptor {
    final List<Class<?>>                          interfaces;
    final Object                                  fallbackObject;
    final HashMap<MethodSignature, MethodHandler> methodHandlers;

    public Descriptor(List<Class<?>> interfaces, Map<MethodSignature, MethodHandler> methodHandlers, Object fallbackObject) {
      this.methodHandlers = new HashMap<>(Objects.requireNonNull(methodHandlers));
      this.interfaces = new LinkedList<>(Objects.requireNonNull(interfaces));
      this.fallbackObject = Objects.requireNonNull(fallbackObject);
    }

    public List<Class<?>> interfaces() {
      return unmodifiableList(this.interfaces);
    }

    public Object fallbackObject() {
      return this.fallbackObject;
    }

    public Map<MethodSignature, MethodHandler> methodHandlers() {
      return unmodifiableMap(this.methodHandlers);
    }

    public static class Builder {
      final List<Class<?>>                          interfaces;
      final HashMap<MethodSignature, MethodHandler> methodHandlers;
      Object fallbackObject;
      public Builder() {
        interfaces = new LinkedList<>();
        methodHandlers = new HashMap<>();
      }

      public Builder(Descriptor descriptor) {
        this();
        this.interfaces.addAll(descriptor.interfaces());
        this.methodHandlers.putAll(descriptor.methodHandlers());
        this.fallbackObject = descriptor.fallbackObject;
      }

      public Builder fallbackObject(Object fallbackObject) {
        this.fallbackObject = fallbackObject;
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
        return new Descriptor(this.interfaces, this.methodHandlers, this.fallbackObject);
      }

      public List<Class<?>> interfaces() {
        return unmodifiableList(this.interfaces);
      }
    }
  }
}
