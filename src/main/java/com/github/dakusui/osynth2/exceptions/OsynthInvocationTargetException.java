package com.github.dakusui.osynth2.exceptions;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class OsynthInvocationTargetException extends OsynthException {
  private final Throwable targetException;

  public OsynthInvocationTargetException(InvocationTargetException e) {
    super(e);
    this.targetException = Objects.requireNonNull(e.getTargetException());
  }

  public Throwable targetException() {
    return this.targetException;
  }
}
