package com.github.dakusui.osynth.core;

import java.lang.reflect.Method;

public interface AutoLogger {

  static MethodHandlerDecorator create(AutoLogger autoLogger) {
    return (method, methodHandler) -> (MethodHandler) (synthesizedObject, args) -> {
      autoLogger.log(autoLogger.enter(synthesizedObject, method, args));
      Object ret;
      try {
        ret = methodHandler.handle(synthesizedObject, args);
        autoLogger.log(autoLogger.leave(synthesizedObject, method, ret));
        return ret;
      } catch (Throwable t) {
        autoLogger.log(autoLogger.exception(synthesizedObject, method, t));
        throw t;
      }
    };
  }

  interface Entry {


    enum Type {
      ENTER("arguments") {
      },
      LEAVE("return") {
      },
      EXCEPTION("exception") {
      };

      private final String outputValueLabel;

      Type(String outputValueLabel) {
        this.outputValueLabel = outputValueLabel;
      }

      public String outputValueLabel() {
        return this.outputValueLabel;
      }
    }

    Entry.Type type();

    Object object();

    Method method();

    Object value();

    /**
     * A default implementation of this method is provided to save implementation
     * effort for entries not need arguments.
     *
     * @return An argument array passed this entry when this represents a method entry.
     */
    default Object[] arguments() {
      throw new UnsupportedOperationException();
    }

    /**
     * A default implementation of this method is provided to save implementation
     * effort for entries not need exception.
     *
     * @return An exception detected during method execution.
     */
    default Throwable exception() {
      throw new UnsupportedOperationException();
    }

    ;

    static Entry enter(Object object, Method method, Object[] args) {
      return new Entry() {
        @Override
        public Type type() {
          return Type.ENTER;
        }

        @Override
        public Object object() {
          return object;
        }

        @Override
        public Method method() {
          return method;
        }

        @Override
        public Object[] arguments() {
          return args;
        }

        @Override
        public Object value() {
          return this.arguments();
        }
      };
    }

    static Entry leave(Object object, Method method, Object returnedValue) {
      return new Entry() {
        @Override
        public Type type() {
          return Type.LEAVE;
        }

        @Override
        public Object object() {
          return object;
        }

        @Override
        public Method method() {
          return method;
        }

        @Override
        public Object value() {
          return returnedValue;
        }
      };
    }

    static Entry exception(Object object, Method method, Throwable exception) {
      return new Entry() {
        @Override
        public Type type() {
          return Type.EXCEPTION;
        }

        @Override
        public Object object() {
          return object;
        }

        @Override
        public Method method() {
          return method;
        }

        @Override
        public Object value() {
          return exception();
        }

        @Override
        public Throwable exception() {
          return exception;
        }
      };
    }
  }

  default Entry enter(Object object, Method method, Object[] args) {
    return Entry.enter(object, method, args);
  }

  default Entry leave(Object object, Method method, Object returnedValue) {
    return Entry.leave(object, method, returnedValue);
  }

  default Entry exception(Object object, Method method, Throwable t) {
    return Entry.exception(object, method, t);
  }

  void log(Entry entry);
}
