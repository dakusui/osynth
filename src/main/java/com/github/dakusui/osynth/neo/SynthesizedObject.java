package com.github.dakusui.osynth.neo;

import com.github.dakusui.osynth.neo.BuiltInHandlerFactory.ForDescriptor;
import com.github.dakusui.osynth.neo.BuiltInHandlerFactory.ForEquals;
import com.github.dakusui.osynth.neo.BuiltInHandlerFactory.ForHashCode;
import com.github.dakusui.osynth.neo.BuiltInHandlerFactory.ForToString;

import java.util.*;

import static com.github.dakusui.osynth.neo.SynthesizedObject.PrivateUtils.reservedMethodSignatures;
import static com.github.dakusui.pcond.Preconditions.requireNonNull;
import static java.util.stream.Collectors.toSet;

public interface SynthesizedObject {
  Set<MethodSignature> RESERVED_METHOD_SIGNATURES = reservedMethodSignatures();

  @BuiltInHandlerFactory(ForDescriptor.class)
  @ReservedByOSynth
  Descriptor descriptor();

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

  @BuiltInHandlerFactory(ForEquals.class)
  @Override
  boolean equals(Object object);

  @BuiltInHandlerFactory(ForHashCode.class)
  @Override
  int hashCode();

  @BuiltInHandlerFactory(ForToString.class)
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
  }

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

    private Map<MethodSignature, ? extends MethodHandler> methodHandlers() {
      return Collections.unmodifiableMap(this.methodHandlers);
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
