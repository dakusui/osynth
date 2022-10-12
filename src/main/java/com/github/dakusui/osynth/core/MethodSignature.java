package com.github.dakusui.osynth.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * An interface that represents a signature of a method.
 */
public interface MethodSignature {
  /**
   * Creates an instance of this interface from a given `name` and `parameterTypes`.
   *
   * @param name           A name of a method.
   * @param parameterTypes An array of a parameter types.
   * @return A method signature object.
   */
  static MethodSignature create(String name, Class<?>... parameterTypes) {
    return new Impl(name, parameterTypes);
  }

  /**
   * Creates an instance of this interface from a `Method` object.
   *
   * @param method A method object from which an instance of this interface is created.
   * @return A method signature object.
   */
  static MethodSignature create(Method method) {
    return create(method.getName(), method.getParameterTypes());
  }

  static String formatSignature(String methodName, Class<?>[] parameterTypess) {
    return String.format("%s(%s)", methodName, Arrays.stream(parameterTypess).map(Class::getSimpleName).collect(joining(",")));
  }

  /**
   * A name of a method represented by this interface.
   *
   * @return A name of method.
   */
  String name();

  /**
   * An array of parameter types of a method represented by this interface.
   *
   * @return An array of parameter types of a method.
   */
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
      return formatSignature(this.name, this.parameterTypes);
    }
  }
}
