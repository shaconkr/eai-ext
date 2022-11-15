package kr.shacon.edi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Generates the regular expression the matches the {@link RecordDefinition}
 */
public class RegexGenerator {

  private final Logger log = Logger.getLogger(RegexGenerator.class.getName());

  private final Map<RecordDefinition, Pattern> deepPatterns;
  private final Map<RecordDefinition, Pattern> localPatterns;

  public RegexGenerator() {
    deepPatterns = new HashMap<>();
    localPatterns = new HashMap<>();
  }

  private void addFiller(StringBuilder sb, int rowLengthByDefinition, int actualRowLength) {
    int fillerLength = rowLengthByDefinition - actualRowLength;
    if (fillerLength > 0) {
      sb.append("[ ]{").append(fillerLength).append("}");
    }
  }

  public Pattern deepPattern(RecordDefinition definition) {
    if (!deepPatterns.containsKey(definition)) {
      StringBuilder sb = new StringBuilder();
      deepPattern(definition, sb);
      deepPatterns.put(definition, Pattern.compile(sb.toString(), Pattern.MULTILINE));
      log.log(Level.FINE, () -> "Generated regex: " + deepPatterns.get(definition).toString());
    }
    return deepPatterns.get(definition);
  }

  private void deepPattern(RecordDefinition definition, StringBuilder sb) {
    // Open the choice parentheses
    if (definition.isChoice()) {
      sb.append("(");
    }

    localPattern(definition, sb);

    for (Iterator<RecordDefinition> iter = definition.getSubRecords().iterator(); iter.hasNext(); ) {
      RecordDefinition subRecord = iter.next();
      // Open the grouping parentheses
      sb.append("(");
      deepPattern(subRecord, sb);
      // Close the grouping parentheses and open the repeat bracket, and add the
      // min occurs count
      sb.append("){").append(subRecord.getMinOccurs()).append(",");

      // It maxOccurs is defined, add it
      if (subRecord.getMaxOccurs() != -1) {
        sb.append(subRecord.getMaxOccurs());
      }
      // Close the repeat bracket
      sb.append("}");

      // If we have more choice recors, add the choice pipe char
      if (definition.isChoice() && iter.hasNext()) {
        sb.append(")|(");
      }
    }

    // Close the choice parentheses
    if (definition.isChoice()) {
      sb.append(")");
    }

  }

  public Pattern localPattern(RecordDefinition definition) {
    if (!localPatterns.containsKey(definition)) {
      StringBuilder sb = new StringBuilder();
      localPattern(definition, sb);
      localPatterns.put(definition, Pattern.compile(sb.toString(), Pattern.MULTILINE));
    }
    return localPatterns.get(definition);
  }

  private void localPattern(RecordDefinition definition, StringBuilder sb) {
    if (definition.getProperties().isEmpty()) {
      return;
    }

    // All records start on a line start
//    sb.append("\\n?^");

    int currentRow = 0;
    int actualRowLength = 0;
    for (Iterator<RecordDefinition.Property> iter = definition.getProperties().iterator(); iter.hasNext(); ) {
      RecordDefinition.Property property = iter.next();

      // If multiline, add the filler, start new line and step the line counter
      if (property.getRow() != currentRow) {
        currentRow = property.getRow();
        addFiller(sb, definition.getLength(), actualRowLength);
        actualRowLength = 0;
//        sb.append("\\n?^");  // head/body
      }
      actualRowLength += property.getLength();

      if (property.getFixedValue() != null) {
        sb.append("(").append(property.getFixedValue()).append(")");
      } else if (property.getLength() == 0) {
        sb.append("(").append(definition.getPropertyPattern()).append("*)");
      } else if (property.isEnum()) {
        sb.append("(");
        EnumPropertyHelper enumHelper = new EnumPropertyHelper(property);
        String[] values = enumHelper.getStringValues();
        for (int i = 0; i < values.length; i++) {
          sb.append(values[i]).append("[ ]{").append(property.getLength() - values[i].length()).append("}");
          if (i < values.length - 1) {
            sb.append("|");
          }
        }
        sb.append(")");
      } else {
        sb.append("([\\w\\W]{").append(property.getLength()).append("})");
      }

      if (iter.hasNext() && definition.isDelimited()) {
        sb.append(Pattern.quote(definition.getPropertyDelimiter()));
        actualRowLength++;
      }
    }

    addFiller(sb, definition.getLength(), actualRowLength);
  }
}
