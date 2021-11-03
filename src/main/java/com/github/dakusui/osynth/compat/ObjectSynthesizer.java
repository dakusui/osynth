package com.github.dakusui.osynth.compat;

import com.github.dakusui.osynth.compat.core.MethodHandler;

import java.util.LinkedList;
import java.util.List;

import static com.github.dakusui.osynth.utils.Messages.notAnInterface;
import static com.github.dakusui.pcond.Preconditions.requireNonNull;

public interface ObjectSynthesizer {
  static MethodHandler.Builder methodCall(String methodName, Class<?>... parameterTypes) {
    return MethodHandler.builderByNameAndParameterTypes(requireNonNull(methodName), requireNonNull(parameterTypes));
  }

  ObjectSynthesizer addInterface(Class<?> anInterface);

  ObjectSynthesizer handle(MethodHandler handler);

  <T> T synthesize();

  abstract class Base implements ObjectSynthesizer {
    protected final List<Class<?>>      interfaces = new LinkedList<>();
    protected final List<MethodHandler> handlers   = new LinkedList<>();

    @Override
    public ObjectSynthesizer addInterface(Class<?> anInterface) {
      if (!requireNonNull(anInterface).isInterface())
        throw new IllegalArgumentException(notAnInterface(anInterface));
      if (!this.interfaces.contains(anInterface))
        this.interfaces.add(anInterface);
      return this;
    }

    @Override
    public ObjectSynthesizer handle(MethodHandler handler) {
      this.handlers.add(requireNonNull(handler));
      return this;
    }
  }

  class Impl extends Base {
    @Override
    public <T> T synthesize() {
      return null;
    }
  }
}
