package com.github.dakusui.osynth.exceptions;

import java.lang.reflect.InvocationTargetException;

public class OsynthException extends RuntimeException {
  public OsynthException(Throwable e) {
    super(e.getCause());
  }

  public OsynthException(String message) {
    super(message);
  }

  public OsynthException(String message, Throwable cause) {
    super(message, cause);
  }

  public static OsynthException from(String customMessage, Throwable e) {
    if (e instanceof Error)
      throw (Error) e;
    if (e instanceof InvocationTargetException)
      throw from(customMessage, ((InvocationTargetException) e).getTargetException());
    else if (e instanceof OsynthException)
      if (e.getCause() == null)
        throw (OsynthException) e;
      else
        throw from(customMessage, e.getCause());
    else if (e instanceof RuntimeException)
      throw (RuntimeException)e;
    else {
      if (customMessage == null)
        throw new OsynthException(e);
      else
        throw new OsynthException(customMessage, e);
    }
  }
}
