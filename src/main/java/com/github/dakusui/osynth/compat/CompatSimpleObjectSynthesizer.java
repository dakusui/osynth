package com.github.dakusui.osynth.compat;

import com.github.dakusui.osynth.compat.core.MethodHandler;

public class CompatSimpleObjectSynthesizer<T> extends CompatObjectSynthesizer {

  private final Class<T> primaryInterface;

  public CompatSimpleObjectSynthesizer(Class<T> anInterface) {
    this.primaryInterface = anInterface;
    this.addInterface(anInterface);
  }

  public static <T> CompatSimpleObjectSynthesizer<T> create(Class<T> anInterface) {
    return new CompatSimpleObjectSynthesizer<>(anInterface);
  }

  @Override
  @SuppressWarnings("unchecked")
  public CompatSimpleObjectSynthesizer<T> addHandlerObject(Object handlerObject) {
    return (CompatSimpleObjectSynthesizer<T>) super.addHandlerObject(handlerObject);
  }

  @Override
  @SuppressWarnings("unchecked")
  public CompatSimpleObjectSynthesizer<T> handle(MethodHandler handler) {
    return (CompatSimpleObjectSynthesizer<T>) super.handle(handler);
  }

  @SuppressWarnings("unchecked")
  @Override
  public T synthesize() {
    return super.synthesize(this.primaryInterface);
  }
}
