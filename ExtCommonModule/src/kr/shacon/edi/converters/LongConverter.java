package kr.shacon.edi.converters;

import kr.shacon.edi.Converter;

public class LongConverter implements Converter {

  public Object convert(String value) {
    if (value == null || value.length() == 0) {
      return null;
    }
    return Long.valueOf(value);
  }

  public String toString(Object value) {
    if (value == null) {
      return "";
    }
    return value.toString();
  }

}
