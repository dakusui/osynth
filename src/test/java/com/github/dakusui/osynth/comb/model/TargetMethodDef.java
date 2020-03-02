package com.github.dakusui.osynth.comb.model;

public class TargetMethodDef {
  private final MethodType    methodType;
  private final int           numArgs;
  private final ExceptionType exceptionType;

  public TargetMethodDef(MethodType methodType, int numArgs, ExceptionType exceptionType) {
    this.methodType = methodType;
    this.numArgs = numArgs;
    this.exceptionType = exceptionType;
  }

  public MethodType getMethodType() {
    return methodType;
  }

  public int getNumArgs() {
    return numArgs;
  }

  public ExceptionType getExceptionType() {
    return exceptionType;
  }

  public String methodName() {
    return String.format("apply%s", numArgs);
  }

  public Object[] args() {
    Object[] args = new Object[numArgs];
    for (int i = 0; i < numArgs; i++)
      args[i] = (i + 1) * 100;
    return args;
  }
}
