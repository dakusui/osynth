package com.github.dakusui.osynth.comb.model;

import com.github.dakusui.osynth.MethodHandler;
import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.comb.def.I1N;
import com.github.dakusui.osynth.comb.def.I2N;

import java.util.List;
import java.util.function.BiFunction;

import static com.github.dakusui.crest.Crest.asString;
import static com.github.dakusui.crest.Crest.assertThat;
import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class ObjectSynthesizerWrapper {
  final ObjectSynthesizer objectSynthesizer;

  public ObjectSynthesizerWrapper(ObjectSynthesizer objectSynthesizer) {
    this.objectSynthesizer = objectSynthesizer;
  }

  public static ObjectSynthesizer.FallbackHandlerFactory createFallbackHandlerFactory(MethodType methodType, ExceptionType exceptionType) {
    return methodType.createFallbackHandlerFactory(exceptionType);
  }

  public static MethodHandler createMethodHandler(int numArgs, MethodType methodType, ExceptionType exceptionType) {
    Class<?>[] argTypes = new Class[numArgs];
    for (int i = 0; i < numArgs; i++)
      argTypes[i] = int.class;
    BiFunction<Object, Object[], Object> methodHandlingFunction;
    methodHandlingFunction = methodType.createMethodHandler(argTypes, exceptionType);
    return methodCall(format("apply%s", numArgs), argTypes).with(methodHandlingFunction);
  }

  public ObjectSynthesizerWrapper addInterfaces(TargetMethodDef targetMethodDef, int numInterfaces) {
    Class<?>[] interfaces = targetMethodDef.getMethodType().interfaces(targetMethodDef.getExceptionType());
    for (int i = 0; i < numInterfaces; i++)
      requireNonNull(objectSynthesizer.addInterface(interfaces[i]));
    return this;
  }

  public ObjectSynthesizerWrapper addHandlerObjects(TargetMethodDef targetMethodDef, int numHandlerObjects) {
    List<?> handlerObjects = targetMethodDef.getMethodType().handlerObjects(targetMethodDef.getExceptionType());
    for (int i = 0; i < numHandlerObjects; i++)
      requireNonNull(objectSynthesizer.addHandlerObject(handlerObjects.get(i)));
    return this;
  }

  public ObjectSynthesizerWrapper setFallbackHandlerFactory(TargetMethodDef targetMethodDef, boolean customFallback) {
    if (customFallback)
      requireNonNull(objectSynthesizer.fallbackHandlerFactory(createFallbackHandlerFactory(targetMethodDef.getMethodType(), targetMethodDef.getExceptionType())));
    return this;
  }

  public ObjectSynthesizerWrapper addMethodHandlers(TargetMethodDef targetMethodDef, int numMethodHandlers) {
    for (int i = 0; i < numMethodHandlers; i++)
      requireNonNull(objectSynthesizer.handle(createMethodHandler(targetMethodDef.getNumArgs(), targetMethodDef.getMethodType(), targetMethodDef.getExceptionType())));
    return this;
  }

  public <T> T synthesize() {
    return this.objectSynthesizer.synthesize();
  }

  //  @Ignore
  //  @Test
  public void example() {
    Object obj = new ObjectSynthesizer()
        .handle(ObjectSynthesizerWrapper.createMethodHandler(1, MethodType.NORMAL, null))
        .addHandlerObject(new Object())
        .addInterface(I1N.class)
        .addInterface(I2N.class)
        .fallbackHandlerFactory(ObjectSynthesizerWrapper.createFallbackHandlerFactory(MethodType.NORMAL, null))
        .synthesize();
    assertThat(
        obj,
        asString("apply1", 100).equalTo("apply1").$()
    );
  }

}

