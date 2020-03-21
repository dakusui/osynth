package com.github.dakusui.osynth.utils;

public enum InternalUtils {
  ;

  public static RuntimeException rethrow(Throwable e) {
    if (e instanceof RuntimeException)
      throw (RuntimeException) e;
    if (e instanceof Error)
      throw (Error) e;
    throw new RuntimeException(e);
  }
}
