package com.github.dakusui.osynth.neo;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import static com.github.dakusui.pcond.Preconditions.requireNonNull;

public final class MethodSignature {
  final String     name;
  final Class<?>[] parameterClasses;

  public MethodSignature(String name, Class<?>[] parameterClasses) {
    this.name = requireNonNull(name);
    this.parameterClasses = requireNonNull(parameterClasses);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.name) + Objects.hashCode(this.parameterClasses);
  }

  @Override
  public boolean equals(Object anotherObject) {
    if (!(anotherObject instanceof MethodSignature))
      return false;
    MethodSignature another = (MethodSignature) anotherObject;
    return Objects.equals(this.name, another.name) && Arrays.equals(this.parameterClasses, another.parameterClasses);
  }

  static MethodSignature create(String name, Class<?>[] parameterClasses) {
    return new MethodSignature(name, parameterClasses);
  }

  static MethodSignature create(Method method) {
    return create(method.getName(), method.getParameterTypes());
  }
}
