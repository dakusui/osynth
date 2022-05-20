package com.github.dakusui.osynth.utils;

import com.github.dakusui.pcond.forms.Printables;

import java.util.List;
import java.util.function.Predicate;

public enum TestForms {
  ;

  public static String joinByLineBreak(List<String> out) {
    return String.join(String.format("%n"), out);
  }

  public static Predicate<? super Object> objectIsEqualTo(Object decorator) {
    return Printables.predicate("isEqualTo[" + decorator + "]", v -> v.equals(decorator));
  }
}
