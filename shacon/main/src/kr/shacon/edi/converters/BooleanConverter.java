package kr.shacon.edi.converters;

import kr.shacon.edi.Converter;

public class BooleanConverter implements Converter {

    public Object convert(String value) {
      return "Y".equals(value);
    }

    public String toString(Object value) {
      return (boolean) value ? "Y" : "N";
    }

  }