package kr.shacon.edi.util;

import java.lang.reflect.Method;

/**
 * Given an object, looks for methods that start with "get" or "is" and return a String.
 * Then it replaces that String, trimmed, by getting and setting it
 */
public class Trimmer {

  public void trim(Object obj) throws TrimmerException {
    try {
      for (Method readMethod : obj.getClass().getMethods()) {
        String fieldName = null;
        String readMethodName = readMethod.getName();
        if (readMethodName.startsWith("is")) {
          fieldName = readMethodName.substring(2);
        } else if (readMethodName.startsWith("get")) {
          fieldName = readMethodName.substring(3);
        }

        if (fieldName != null && String.class.isAssignableFrom(readMethod.getReturnType())) {
          Method writeMethod = obj.getClass().getMethod("set" + fieldName, String.class);
          if (writeMethod != null) {
            String value = (String) readMethod.invoke(obj);
            if (value != null) {
              value = value.trim();
              writeMethod.invoke(obj, value);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new TrimmerException(e);
    }
  }
}
