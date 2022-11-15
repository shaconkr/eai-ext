package kr.shacon.edi.converters;

import kr.shacon.edi.Converter;

import java.math.BigDecimal;

public class BigDecimalConverter implements Converter {

    public Object convert(String value) {
      return new BigDecimal(value.trim());
    }

    public String toString(Object value) {
      return ((BigDecimal) value).toPlainString();
    }

  }