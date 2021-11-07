package com.github.dakusui.osynth.annotations;

import com.github.dakusui.osynth.core.MethodHandler;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Inherited
public @interface Synthesize {
  Class<Configurator> with() default Configurator.class;

  MethodHandlerDefinition[] handleMethod() default {};

  @interface MethodHandlerDefinition {
    String name();

    Class<?>[] parameterTypes() default {};

    Class<? extends MethodHandler.Factory> with();
  }
}
