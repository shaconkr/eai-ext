package kr.shacon.edi.converters;

import kr.shacon.edi.Converter;

public class VoidConverter implements Converter {

  public Object convert(String value) {
    return null;
  }

  public String toString(Object value) {
    return "";
  }

}
