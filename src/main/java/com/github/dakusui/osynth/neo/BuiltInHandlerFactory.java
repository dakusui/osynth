package com.github.dakusui.osynth.neo;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface BuiltInHandlerFactory {
  Class<? extends MethodHandlerFactory> value();

  interface MethodHandlerFactory {
    MethodHandler create(SynthesizedObject.Descriptor descriptor);
  }

  class ForToString implements MethodHandlerFactory {
    @Override
    public MethodHandler create(SynthesizedObject.Descriptor descriptor) {
      return (synthesizedObject, objects) -> synthesizedObject.fallbackObject().toString();
    }
  }

  class ForHashCode implements MethodHandlerFactory {
    @Override
    public MethodHandler create(SynthesizedObject.Descriptor descriptor) {
      return (synthesizedObject, objects) -> synthesizedObject.fallbackObject().hashCode();
    }
  }

  class ForEquals implements MethodHandlerFactory {
    @Override
    public MethodHandler create(SynthesizedObject.Descriptor descriptor) {
      return (synthesizedObject, objects) -> synthesizedObject == objects[0];
    }
  }

  class ForDescriptor implements MethodHandlerFactory {
    @Override
    public MethodHandler create(SynthesizedObject.Descriptor descriptor) {
      return (synthesizedObject, objects) -> descriptor;
    }
  }
}
