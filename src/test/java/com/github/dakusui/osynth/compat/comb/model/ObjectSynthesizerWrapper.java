package com.github.dakusui.osynth.compat.comb.model;

import com.github.dakusui.osynth.compat.testwrappers.LegacyObjectSynthesizer;
import com.github.dakusui.osynth.compat.comb.def.I1N;
import com.github.dakusui.osynth.compat.comb.def.I2N;
import com.github.dakusui.osynth.core.MethodHandler;
import com.github.dakusui.osynth.core.MethodHandlerEntry;

import java.util.List;

import static com.github.dakusui.crest.Crest.asString;
import static com.github.dakusui.crest.Crest.assertThat;
import static com.github.dakusui.osynth.compat.testwrappers.LegacyObjectSynthesizer.methodCall;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class ObjectSynthesizerWrapper {
  final LegacyObjectSynthesizer objectSynthesizer;
  final MethodHandlerEntry[]    methodHandlersRegisteredAlways = new MethodHandlerEntry[] {
      methodCall("apply0_both").with((o, objects) -> "apply0_both:I1:methodHandler"),
      methodCall("apply0_both").with((o, objects) -> "apply0_both:I2:methodHandler")
  };
  final MethodHandlerEntry[] methodHandlersForEachInterface = new MethodHandlerEntry[] {
      methodCall("apply0_1").with((o, objects) -> "apply0_1:I1:methodHandler"),
      methodCall("apply0_2").with((o, objects) -> "apply0_2:I2:methodHandler")
  };

  public ObjectSynthesizerWrapper(LegacyObjectSynthesizer objectSynthesizer) {
    this.objectSynthesizer = objectSynthesizer;
  }

  public static MethodHandlerEntry createMethodHandler(int numArgs, MethodType methodType, ExceptionType exceptionType) {
    Class<?>[] argTypes = new Class[numArgs];
    for (int i = 0; i < numArgs; i++)
      argTypes[i] = int.class;
    /*BiFunction<Object, Object[], Object>*/
    MethodHandler methodHandlingFunction;
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
      requireNonNull(objectSynthesizer.fallbackTo(handlerObjects.get(i)));
    return this;
  }

  public ObjectSynthesizerWrapper addMethodHandlers(TargetMethodDef targetMethodDef, int numMethodHandlers) {
    for (MethodHandlerEntry each : methodHandlersRegisteredAlways)
      objectSynthesizer.handle(each);
    for (int i = 0; i < numMethodHandlers; i++) {
      requireNonNull(objectSynthesizer.handle(createMethodHandler(targetMethodDef.getNumArgs(), targetMethodDef.getMethodType(), targetMethodDef.getExceptionType())));
      objectSynthesizer.handle(methodHandlersForEachInterface[i]);
    }
    return this;
  }

  public <T> T synthesize(Class<T> klass) {
    return this.objectSynthesizer.synthesize().castTo(klass);
  }

  private static void example() {
    Object obj = new LegacyObjectSynthesizer()
        .handle(ObjectSynthesizerWrapper.createMethodHandler(1, MethodType.NORMAL, null))
        .fallbackTo(new Object())
        .addInterface(I1N.class)
        .addInterface(I2N.class)
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

