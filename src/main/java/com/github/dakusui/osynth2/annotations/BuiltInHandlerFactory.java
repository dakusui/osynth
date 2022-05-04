package com.github.dakusui.osynth2.annotations;

import com.github.dakusui.osynth2.core.MethodHandler;
import com.github.dakusui.osynth2.core.SynthesizedObject;

import java.lang.annotation.Retention;
import java.util.function.Supplier;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface BuiltInHandlerFactory {
  Class<? extends MethodHandlerFactory> value();

  interface MethodHandlerFactory {
    MethodHandler create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier);
  }

  class ForToString implements MethodHandlerFactory {
    @Override
    public MethodHandler create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> composeFormattedString(synthesizedObject);
    }

    private static String composeFormattedString(SynthesizedObject synthesizedObject) {
      return String.format("osynth:%s", synthesizedObject.descriptor());
    }
  }

  class ForHashCode implements MethodHandlerFactory {
    @Override
    public MethodHandler create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> synthesizedObject.descriptor().fallbackObject().hashCode();
    }
  }

  class ForEquals implements MethodHandlerFactory {
    @Override
    public MethodHandler create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> synthesizedObject == objects[0];
    }
  }

  class ForDescriptor implements MethodHandlerFactory {
    @Override
    public MethodHandler create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> descriptorSupplier.get();
    }
  }
}
