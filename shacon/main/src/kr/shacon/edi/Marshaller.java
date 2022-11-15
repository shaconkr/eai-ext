package kr.shacon.edi;

import kr.shacon.edi.padders.SpaceRightPadder;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Transforms beans to text according to the given record definition
 */
public class Marshaller<E> extends AbstractUnMarshaller {

  /**
   * Creates a new marshaller, with the specified record definition
   *
   * @param definition the record definition
   * @see SpaceRightPadder
   */
  public Marshaller(RecordDefinition definition) {
    this(definition, new HashMap<>(), new HashMap<>());
  }

  /**
   * Creates a new marshaller, with the specified record definition and user provided instances of converters and padders.
   *
   * @param definition the record definition
   * @param converters user provided instances of converters
   * @param padders    user provided instances of padders
   */
  public Marshaller(RecordDefinition definition, Map<String, Converter> converters, Map<String, Padder> padders) {
    super(definition, converters, padders);
  }

  private void fillWithBlanks(StringBuilder sb, int definitionLength, int length) {
    int fillerLength = definitionLength - length;
    while (fillerLength-- > 0) {
      sb.append(" ");
    }
  }

  private Object ensureCorrectLength(int length, String value) {
    if (length > 0 && value.length() > length) {
      return value.substring(0, length);
    }
    return value;
  }

  /**
   * Marshalls a bean to a writer
   *
   * @param record the bean to marshal
   * @param writer the target writer
   */
  public void marshall(Map<String,Object> record, Writer writer) {
    marshall(record, definition, writer);
  }

  private void marshall(Map<String,Object> record, RecordDefinition currentDefinition, Writer writer) {
    StringBuilder sb = new StringBuilder(currentDefinition.getLength());
    int currentRow = 0;
    int length = 0;
    boolean propertyFound = false;
    for (Iterator<RecordDefinition.Property> iter = currentDefinition.getProperties().iterator(); iter.hasNext(); ) {
      propertyFound = true;
      RecordDefinition.Property property = iter.next();
      if (property.getRow() != currentRow) {
        currentRow = property.getRow();
        fillWithBlanks(sb, currentDefinition.getLength(), length);
        length = 0;
        sb.append(definition.getLineSeparator());
      }
      String propertyValue = getPropertyValue(record, currentDefinition, property);
      sb.append(ensureCorrectLength(property.getLength(), propertyValue));
      length += property.getLength();
      if (iter.hasNext()) {
        sb.append(currentDefinition.getPropertyDelimiter());
        length += currentDefinition.getPropertyDelimiter().length();
      }
    }

    if (propertyFound) {
      fillWithBlanks(sb, currentDefinition.getLength(), length);

//      sb.append(definition.getLineSeparator());
    }

    try {
      writer.append(sb.toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (currentDefinition.isChoice()) {
      Optional<RecordDefinition> choiceSubDefinition = currentDefinition.getSubRecords().stream()
        .filter(subDefinition -> {
          Object subRecord = propertyUtils.getProperty(record, subDefinition.getSetterName());
          return subRecord != null && subRecord.getClass().getName().equals(subDefinition.getClassName());
        })
        .findFirst();

      if (choiceSubDefinition.isPresent()) {
        RecordDefinition subDefinition = choiceSubDefinition.get();
        Map<String,Object> subRecord = (Map<String,Object>)propertyUtils.getProperty(record, subDefinition.getSetterName());

        marshallSubRecord(subRecord, subDefinition, writer);
      }
    } else {
      for (RecordDefinition subDefinition : currentDefinition.getSubRecords()) {
        Map<String,Object> subRecord = (Map<String,Object>)propertyUtils.getProperty(record, subDefinition.getSetterName());
        if (subRecord == null && !currentDefinition.isChoice()) {
          throw new NullPointerException("Missing object from " + subDefinition.getSetterName());
        }
        marshallSubRecord(subRecord, subDefinition, writer);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void marshallSubRecord(Map<String,Object> subRecord, RecordDefinition subDefinition, Writer writer) {
    if (subRecord != null) {
      if (subRecord instanceof Collection) {
        Collection<Map<String,Object>> subRecords = (Collection<Map<String,Object>>) subRecord;
        for (Map<String,Object> o : subRecords) {
          marshall(o, subDefinition, writer);
        }
      } else {
        marshall(subRecord, subDefinition, writer);
      }
    }
  }

  private String getPropertyValue(Map<String,Object> record, RecordDefinition currentDefinition, RecordDefinition.Property property) {
    if (property.getFixedValue() != null) {
      return property.getFixedValue();
    }

    Object propertyValue = propertyUtils.getProperty(record, property.getName());
    String convertedPropertyValue = getConverter(property).toString(propertyValue);
    String paddedPropertyValue = getPadder(currentDefinition, property).pad(convertedPropertyValue, property.getLength());

    return paddedPropertyValue;
  }

  /**
   * Marshalls a collection of beans to a writer
   *
   * @param records the beans to marshall
   * @param writer  the target writer
   */
  public void marshallAll(Collection<Map<String,Object>> records, Writer writer) {
    for (Map<String,Object> record : records) {
      marshall(record, definition, writer);
    }
  }
}
