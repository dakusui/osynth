package com.github.dakusui.osynth.utils;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.SynthesizedObject;
import com.github.dakusui.osynth.exceptions.OsynthException;
import com.github.dakusui.pcond.forms.Printables;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.dakusui.crest.Crest.predicate;
import static com.github.dakusui.thincrest_pcond.functions.Printable.function;

public enum TestForms {
  ;

  public static String joinByLineBreak(List<String> out) {
    return String.join(String.format("%n"), out);
  }

  public static Predicate<? super Object> objectIsEqualTo(Object decorator) {
    return Printables.predicate("isEqualTo[" + decorator + "]", v -> v.equals(decorator));
  }

  public static Predicate<ObjectSynthesizer> objectSynthesizerIsDescriptorFinalized() {
    return predicate("objectSynthesizerIsDescriptorFinalized", ObjectSynthesizer::isDescriptorFinalized);
  }

  public static Function<String, Integer> integerParseInt() {
    return function("integerParseInt", Integer::parseInt);
  }

  public static Function<Integer, String> objectToString() {
    return function("objectToString", Object::toString);
  }

  public static Function<Object, Integer> objectHashCode() {
    return function("objectHashCode", Object::hashCode);
  }

  public static Function<Throwable, ? extends Throwable> throwableGetCause() {
    return Printables.function("throwableGetCause", Throwable::getCause);
  }

  public static Predicate<Throwable> objectIsSameReferenceAs(Object object) {
    return Printables.predicate("isSameObject[System.identityHash:" + System.identityHashCode(object) + "]", v -> v == object);
  }

  public static Function<Throwable, String> throwableGetMessage() {
    return Printables.function("throwableGetMessage", Throwable::getMessage);
  }

  public static <T> Function<SynthesizedObject, T> synthesizedObjectCastTo(Class<T> classInUse) {
    return Printables.function("castTo[" + classInUse.getCanonicalName() + "]", v -> v.castTo(classInUse));
  }
}
