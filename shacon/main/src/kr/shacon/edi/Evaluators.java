package kr.shacon.edi;

import com.sun.xml.bind.api.impl.NameConverter;
import com.sun.xml.xsom.*;

import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

import static kr.shacon.edi.ShaConstants.SHACON_KR_XSD;

class Evaluators {

  static final Logger log = Logger.getLogger(Evaluators.class.getName());

  static final class Converter implements Evaluator<RecordDefinition.Property, XSElementDecl> {

    public void eval(RecordDefinition.Property target, XSElementDecl source) {
      String converter = source.getForeignAttribute(SHACON_KR_XSD, "converter");
      String defaultConverter = "kr.shacon.edi.converters." + target.getType().substring(target.getType().lastIndexOf(".")+1) + "Converter";
      converter = Objects.requireNonNullElse(converter, defaultConverter);
      target.setConverter(converter);
    }
  }

  static final class Delimiter implements Evaluator<RecordDefinition, XSElementDecl> {

    public void eval(RecordDefinition target, XSElementDecl source) {
      target.setPropertyDelimiter(source.getForeignAttribute(SHACON_KR_XSD, "delimiter"));
    }

  }

  static final class ElementType implements Evaluator<RecordDefinition.Property, XSElementDecl> {

    private final Type type;

    public ElementType() {
      type = new Type();
    }

    public void eval(RecordDefinition.Property target, XSElementDecl source) {
      type.eval(target, source.getType());
    }

  }

  static final class EnumerationRestriction implements Evaluator<RecordDefinition.Property, XSSimpleType> {

    public void eval(RecordDefinition.Property target, XSSimpleType source) {
      String packageName = NameConverter.standard.toPackageName(source.getTargetNamespace());
      List<XSFacet> facets = source.getFacets("enumeration");
      if (!facets.isEmpty()) {
        target.setType(packageName + "." + NameConverter.standard.toClassName(source.getName()));
      }

      String converter = source.getForeignAttribute(SHACON_KR_XSD, "converter");
      if (converter != null) {
        target.setConverter(converter);
      }
    }
  }

  static final class FixedValue implements Evaluator<RecordDefinition.Property, XSElementDecl> {

    public void eval(RecordDefinition.Property target, XSElementDecl source) {
      XmlString fixedValue = source.getFixedValue();
      if (fixedValue != null) {
        target.setFixedValue(fixedValue.value);
      }
    }
  }

  static final class DefaultPadder implements Evaluator<RecordDefinition, XSElementDecl> {

    public void eval(RecordDefinition target, XSElementDecl source) {
      target.setDefaultPadder(source.getForeignAttribute(SHACON_KR_XSD, "padder"));
    }

  }

  static final class PropertyLength implements Evaluator<RecordDefinition.Property, XSElementDecl> {

    public void eval(RecordDefinition.Property target, XSElementDecl source) {
      String length = source.getForeignAttribute(SHACON_KR_XSD, "length");
      if (length != null) {
        target.setLength(Integer.parseInt(length));
      }
    }
  }

  static final class RecordLength implements Evaluator<RecordDefinition, XSElementDecl> {

    public void eval(RecordDefinition target, XSElementDecl source) {
      String length = source.getForeignAttribute(SHACON_KR_XSD, "length");
      if (length != null) {
        target.setLength(Integer.parseInt(length));
      }
    }

  }

  static final class RecordLineSeparator implements Evaluator<RecordDefinition, XSElementDecl> {

    public void eval(RecordDefinition target, XSElementDecl source) {
      String separator = source.getForeignAttribute(SHACON_KR_XSD, "lineSeparator");
      separator = separator != null ? separator : "\n";
      target.setLineSeparator(separator);
    }

  }

  static final class MinMaxOccurs implements Evaluator<RecordDefinition, XSParticle> {

    public void eval(RecordDefinition target, XSParticle source) {
      target.setMinOccurs(source.getMinOccurs() != null ? source.getMinOccurs().intValue() : 1);
      target.setMaxOccurs(source.getMaxOccurs() != null ? source.getMaxOccurs().intValue() : 1);
    }

  }

  static final class Padder implements Evaluator<RecordDefinition.Property, XSElementDecl> {

    public void eval(RecordDefinition.Property target, XSElementDecl source) {
      target.setPadder(source.getForeignAttribute(SHACON_KR_XSD, "padder"));
    }
  }

  static final class Row implements Evaluator<RecordDefinition.Property, XSElementDecl> {

    public void eval(RecordDefinition.Property target, XSElementDecl source) {
      String row = source.getForeignAttribute(SHACON_KR_XSD, "row");
      row = row != null ? row : "0";
      target.setRow(Integer.parseInt(row));
    }
  }

  static final class SimpleRestriction implements Evaluator<RecordDefinition.Property, XSSimpleType> {

    private final Type type;

    public SimpleRestriction() {
      type = new Type();
    }

    public void eval(RecordDefinition.Property target, XSSimpleType source) {
      if (source.getFacets("enumeration").isEmpty()) {
        type.eval(target, source.getBaseType());
        String defaultConverter = "kr.shacon.edi.converters." + target.getType().substring(target.getType().lastIndexOf(".")+1) + "Converter";
        target.setConverter(defaultConverter);
      }
    }

  }

  static final class SubClass implements Evaluator<RecordDefinition, XSComplexType> {

    public void eval(RecordDefinition target, XSComplexType source) {
      String packageName = NameConverter.standard.toPackageName(source.getTargetNamespace());
      String subclass = source.getForeignAttribute(SHACON_KR_XSD, "subclass");
      if (subclass != null) {
        target.setClassName(subclass);
      } else {
        target.setClassName(NameConverter.standard.toClassName(source.getName()), packageName);
      }
    }
  }

  static final class Type implements Evaluator<RecordDefinition.Property, XSType> {

    private static final Map<String, String> XML_TYPE_TO_JAVA_TYPE = new HashMap<>();

    static {
      XML_TYPE_TO_JAVA_TYPE.put("double", Double.class.getSimpleName());
      XML_TYPE_TO_JAVA_TYPE.put("byte", Byte.class.getSimpleName());
      XML_TYPE_TO_JAVA_TYPE.put("string", String.class.getSimpleName());
      XML_TYPE_TO_JAVA_TYPE.put("duration", Duration.class.getName());
      XML_TYPE_TO_JAVA_TYPE.put("int", Integer.class.getSimpleName());
      XML_TYPE_TO_JAVA_TYPE.put("QName", QName.class.getName());
      XML_TYPE_TO_JAVA_TYPE.put("integer", BigInteger.class.getName());
      XML_TYPE_TO_JAVA_TYPE.put("boolean", Boolean.class.getSimpleName());
      XML_TYPE_TO_JAVA_TYPE.put("long", Long.class.getSimpleName());
      XML_TYPE_TO_JAVA_TYPE.put("anyType", Object.class.getSimpleName());
      XML_TYPE_TO_JAVA_TYPE.put("float", Float.class.getSimpleName());
      XML_TYPE_TO_JAVA_TYPE.put("decimal", BigDecimal.class.getName());
      XML_TYPE_TO_JAVA_TYPE.put("base64Binary", "byte[]");
      XML_TYPE_TO_JAVA_TYPE.put("short", Short.class.getName());
      XML_TYPE_TO_JAVA_TYPE.put("date", Date.class.getName());
      XML_TYPE_TO_JAVA_TYPE.put("dateTime", Date.class.getName());
    }

    public void eval(RecordDefinition.Property target, XSType source) {
      target.setType(toJavaType(source.getName()));
    }

    private String toJavaType(String name) {
      if (!XML_TYPE_TO_JAVA_TYPE.containsKey(name)) {
        throw new IllegalArgumentException(name);
      }

      return XML_TYPE_TO_JAVA_TYPE.get(name);
    }
  }

  private final List<Evaluator<RecordDefinition, XSElementDecl>> mainElementEvaluators;
  private final List<Evaluator<RecordDefinition.Property, XSElementDecl>> propertiesEvaluators;
  private final List<Evaluator<RecordDefinition.Property, XSSimpleType>> simpleTypeEvaluators;
  private final List<Evaluator<RecordDefinition.Property, XSElementDecl>> simpleTypePropertiesEvaluators;
  private final List<Evaluator<RecordDefinition, XSParticle>> subRecordsEvaluators;
  private final List<Evaluator<RecordDefinition, XSComplexType>> typeEvaluators;

  public Evaluators() {
    propertiesEvaluators = new ArrayList<>();
    propertiesEvaluators.add(new Row());
    propertiesEvaluators.add(new FixedValue());
    propertiesEvaluators.add(new ElementType());
    propertiesEvaluators.add(new PropertyLength());
    propertiesEvaluators.add(new Converter());
    propertiesEvaluators.add(new Padder());

    mainElementEvaluators = new ArrayList<>();
    mainElementEvaluators.add(new Delimiter());
    mainElementEvaluators.add(new DefaultPadder());
    mainElementEvaluators.add(new RecordLength());
    mainElementEvaluators.add(new RecordLineSeparator());

    typeEvaluators = new ArrayList<>();
    typeEvaluators.add(new SubClass());

    simpleTypeEvaluators = new ArrayList<>();
    simpleTypeEvaluators.add(new EnumerationRestriction());
    simpleTypeEvaluators.add(new SimpleRestriction());

    simpleTypePropertiesEvaluators = new ArrayList<>();
    simpleTypePropertiesEvaluators.add(new PropertyLength());
    simpleTypePropertiesEvaluators.add(new Padder());
    simpleTypePropertiesEvaluators.add(new Converter());

    subRecordsEvaluators = new ArrayList<>();
    subRecordsEvaluators.add(new MinMaxOccurs());
  }

  public List<Evaluator<RecordDefinition, XSElementDecl>> mainElementEvaluators() {
    return mainElementEvaluators;
  }

  public List<Evaluator<RecordDefinition.Property, XSElementDecl>> propertiesEvaluators() {
    return propertiesEvaluators;
  }

  public List<Evaluator<RecordDefinition.Property, XSSimpleType>> simpleTypeEvaluators() {
    return simpleTypeEvaluators;
  }

  public List<Evaluator<RecordDefinition.Property, XSElementDecl>> simpleTypePropertyEvaluators() {
    return simpleTypePropertiesEvaluators;
  }

  public List<Evaluator<RecordDefinition, XSParticle>> subRecordsEvaluators() {
    return subRecordsEvaluators;
  }

  public List<Evaluator<RecordDefinition, XSComplexType>> typeEvaluators() {
    return typeEvaluators;
  }

}
