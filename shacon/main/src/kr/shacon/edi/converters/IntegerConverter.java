package kr.shacon.edi.converters;

import kr.shacon.edi.Converter;

public class IntegerConverter implements Converter {

  public Object convert(String value) {
    if (value == null || value.length() == 0) {
      return null;
    }
    return Integer.valueOf(value);
  }

  public String toString(Object value) {
    if (value == null) {
      return "";
    }
    return value.toString();
  }

}
