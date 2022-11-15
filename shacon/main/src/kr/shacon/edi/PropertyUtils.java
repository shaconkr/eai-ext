package kr.shacon.edi;

import com.sun.xml.bind.api.impl.NameConverter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class PropertyUtils {

  private final Logger log = Logger.getLogger(PropertyUtils.class.getName());

  private final Map<Class<?>, Map<String, Method>> getters;
  private final Map<Class<?>, Map<String, Method>> setters;

  public PropertyUtils() {
    this.getters = new HashMap<>();
    this.setters = new HashMap<>();
  }

  Object getProperty(Map<String,Object> record, String name) {
    log.log(Level.FINE, () -> "Getting property " + name + " from object of " + record.getClass());

    return record.get(name);
//    Class<?> clazz = record.getClass();
//    if (!getters.containsKey(clazz)) {
//      populateMethodsMap(clazz);
//    }
//
//    try {
//      return getters.get(clazz).get(name).invoke(record);
//    } catch (Exception e) {
//      throw new IllegalArgumentException(e);
//    }
  }

  private void populateMethodsMap(Class<?> clazz) {
    Map<String, Method> getters = new HashMap<>();
    Map<String, Method> setters = new HashMap<>();

    for (Method method : clazz.getMethods()) {
      String methodName = method.getName();
      if (methodName.startsWith("is")) {
        getters.put(NameConverter.standard.toVariableName(methodName.substring(2)), method);
      } else if (methodName.startsWith("get")) {
        getters.put(NameConverter.standard.toVariableName(methodName.substring(3)), method);
      } else if (methodName.startsWith("set")) {
        setters.put(NameConverter.standard.toVariableName(methodName.substring(3)), method);
      }
    }
    this.getters.put(clazz, getters);
    this.setters.put(clazz, setters);
  }

  void setProperty(Map<String,Object> record, String name, Object value) {
    log.log(Level.FINE, () -> "Setting property " + name + " with value '" + value + "' to object of " + record.getClass());

    record.put(name,value);
//    Class<?> clazz = record.getClass();
//    if (!setters.containsKey(clazz)) {
//      populateMethodsMap(clazz);
//    }
//
//    try {
//      setters.get(clazz).get(name).invoke(record, value);
//    } catch (Exception e) {
//      throw new IllegalArgumentException(e);
//    }
  }

}
