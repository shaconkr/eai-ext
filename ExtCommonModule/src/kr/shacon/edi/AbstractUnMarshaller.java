package kr.shacon.edi;

import kr.shacon.edi.converters.StringConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static kr.shacon.edi.Utils.*;

abstract class AbstractUnMarshaller {

  protected final Map<String, Converter> converters;
  protected final Map<String, Padder> padders;
  protected final RecordDefinition definition;
  protected final PropertyUtils propertyUtils;

  public AbstractUnMarshaller(RecordDefinition definition, Map<String, Converter> converters, Map<String, Padder> padders) {
    this.definition = definition;
    this.propertyUtils = new PropertyUtils();

    List<RecordDefinition.Property> properties = collectProperties(this.definition);

    this.converters = new HashMap<>();
    this.converters.putAll(converters);
    this.converters.put(StringConverter.class.getName(), new StringConverter());
    createAndStoreMissingInstancesOf(properties, RecordDefinition.Property::getConverter, this.converters);

    this.padders = new HashMap<>();
    this.padders.putAll(padders);
    this.padders.put(this.definition.getDefaultPadder(), (Padder) newInstanceOf(this.definition.getDefaultPadder()));
    createAndStoreMissingInstancesOf(properties, RecordDefinition.Property::getPadder, this.padders);
  }

  /**
   * Gets the padder for the specified property or the default one specified by the record definition
   *
   * @param currentDefinition the record definition
   * @param property          the property
   * @return a padder instance
   */
  protected Padder getPadder(RecordDefinition currentDefinition, RecordDefinition.Property property) {
    if (property.getPadder() != null) {
      return padders.get(property.getPadder());
    }
    return padders.get(currentDefinition.getDefaultPadder());
  }

  /**
   * Gets the converter for the specified property or an instance of {@link StringConverter}
   *
   * @param property the property
   * @return a converter instance
   */
  protected Converter getConverter(RecordDefinition.Property property) {
    if (property.getConverter() != null) {
      return converters.get(property.getConverter());
    }

    return converters.get(StringConverter.class.getName());
  }

}
