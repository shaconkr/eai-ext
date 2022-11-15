package kr.shacon.edi.converters;

import kr.shacon.edi.Converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateConverter implements Converter {

    private final SimpleDateFormat simpleDateFormat;

    public DateConverter() {
      simpleDateFormat = new SimpleDateFormat("yyyyMMdd");  //FIXME  used date time format
    }

    public Object convert(String value) {
      try {
        Calendar instance = Calendar.getInstance();
        instance.setTime(simpleDateFormat.parse(value));
        return instance;
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
    }

    public String toString(Object value) {
      return simpleDateFormat.format(((Calendar) value).getTime());
    }

  }