package kr.shacon.edi.converters;

import kr.shacon.edi.Converter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FloatConverter implements Converter {

    private final DecimalFormat decimalFormat;

    public FloatConverter() {
      this.decimalFormat = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.KOREA));
    }

    public Object convert(String value) {
      return Float.valueOf(value.substring(0, value.length() - 2) + "." + value.substring(value.length() - 2));
    }

    public String toString(Object value) {
      if (value == null) {
        return "";
      }

      return decimalFormat.format(value).replace(".", "");
    }

  }
