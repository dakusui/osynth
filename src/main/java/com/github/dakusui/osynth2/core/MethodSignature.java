package com.github.dakusui.osynth2.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

public interface MethodSignature {
  static MethodSignature create(String name, Class<?>... parameterTypes) {
    return new Impl(name, parameterTypes);
  }

  static MethodSignature create(Method method) {
    return create(method.getName(), method.getParameterTypes());
  }

  String name();

  Class<?>[] parameterTypes();

  class Impl implements MethodSignature {

    private final String     name;
    private final Class<?>[] parameterTypes;

    public Impl(String name, Class<?>[] parameterTypes) {
      this.name = name;
      this.parameterTypes = parameterTypes;
    }

    @Override
    public String name() {
      return this.name;
    }

    @Override
    public Class<?>[] parameterTypes() {
      return Arrays.copyOf(this.parameterTypes, this.parameterTypes.length);
    }

    @Override
    public int hashCode() {
      return this.name.hashCode();
    }
    @Override
    public boolean equals(Object anotherObject) {
      if (!(anotherObject instanceof MethodSignature))
        return false;
      MethodSignature another = (MethodSignature) anotherObject;
      return Objects.equals(this.name(), another.name()) && Arrays.equals(this.parameterTypes(), another.parameterTypes());
    }

    @Override
    public String toString() {
      return String.format("%s(%s)", this.name, Arrays.stream(this.parameterTypes).map(Class::getSimpleName).collect(joining(",")));
    }
  }
}
