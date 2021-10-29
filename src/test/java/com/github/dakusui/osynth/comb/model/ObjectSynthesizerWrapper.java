package com.github.dakusui.osynth.comb.model;

import com.github.dakusui.osynth.compat.core.FallbackHandlerFactory;
import com.github.dakusui.osynth.compat.core.MethodHandler;
import com.github.dakusui.osynth.compat.CompatObjectSynthesizer;
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
  final CompatObjectSynthesizer objectSynthesizer;
  final MethodHandler[]         methodHandlersRegisteredAlways = new MethodHandler[] {
      methodCall("apply0_both").with((o, objects) -> "apply0_both:I1:methodHandler"),
      methodCall("apply0_both").with((o, objects) -> "apply0_both:I2:methodHandler")
  };
  final MethodHandler[]   methodHandlersForEachInterface = new MethodHandler[] {
      methodCall("apply0_1").with((o, objects) -> "apply0_1:I1:methodHandler"),
      methodCall("apply0_2").with((o, objects) -> "apply0_2:I2:methodHandler")
  };

  public ObjectSynthesizerWrapper(CompatObjectSynthesizer objectSynthesizer) {
    this.objectSynthesizer = objectSynthesizer;
  }

  public static FallbackHandlerFactory createFallbackHandlerFactory(MethodType methodType, ExceptionType exceptionType) {
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
    for (MethodHandler each : methodHandlersRegisteredAlways)
      objectSynthesizer.handle(each);
    for (int i = 0; i < numMethodHandlers; i++) {
      requireNonNull(objectSynthesizer.handle(createMethodHandler(targetMethodDef.getNumArgs(), targetMethodDef.getMethodType(), targetMethodDef.getExceptionType())));
      objectSynthesizer.handle(methodHandlersForEachInterface[i]);
    }
    return this;
  }

  public <T> T synthesize() {
    return this.objectSynthesizer.synthesize();
  }

  private static void example() {
    Object obj = new CompatObjectSynthesizer()
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

  public static void main(String... args) {
    example();
  }
}

