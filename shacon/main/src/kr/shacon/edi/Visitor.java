package kr.shacon.edi;

import com.sun.xml.bind.api.impl.NameConverter;
import com.sun.xml.xsom.*;

class Visitor extends AbstractSchemaVisitor {

  private final Evaluators evaluators;
  private final XSSchema mainSchema;
  private final XSSchemaSet schemas;
  private final RecordDefinition recordDefinition;
  private int numberOfSubRecordsFound;
  private XSModelGroup modelGroup;
  private XSParticle particle;

  public Visitor(Evaluators evaluators, XSSchema mainSchema, XSSchemaSet schemas, RecordDefinition recordDefinition) {
    this.evaluators = evaluators;
    this.mainSchema = mainSchema;
    this.schemas = schemas;
    this.recordDefinition = recordDefinition;
    this.numberOfSubRecordsFound = 0;
  }

  @Override
  public void elementDecl(XSElementDecl element) {
    if (ShaConstants.W3C_SCHEMA.equals(element.getType().getTargetNamespace())) {
      ensureNotPuttingPropertiesAfterSubRecords(element);

      RecordDefinition.Property property = new RecordDefinition.Property(NameConverter.standard.toVariableName(element.getName()));

      for (Evaluator<RecordDefinition.Property, XSElementDecl> p : evaluators.propertiesEvaluators()) {
        p.eval(property, element);
      }

      recordDefinition.getProperties().add(property);
    } else if (mainSchema.getSimpleType(element.getType().getName()) != null) {
      XSSimpleType simpleType = mainSchema.getSimpleType(element.getType().getName());

      RecordDefinition.Property property = new RecordDefinition.Property(element.getName());

      for (Evaluator<RecordDefinition.Property, XSElementDecl> p : evaluators.simpleTypePropertyEvaluators()) {
        p.eval(property, element);
      }
      for (Evaluator<RecordDefinition.Property, XSSimpleType> e : evaluators.simpleTypeEvaluators()) {
        e.eval(property, simpleType);
      }

      recordDefinition.getProperties().add(property);
    } else {
      numberOfSubRecordsFound++;

      RecordDefinition subDefinition = new RecordDefinition(recordDefinition);
      recordDefinition.getSubRecords().add(subDefinition);
      recordDefinition.setChoice(modelGroup != null);

      subDefinition.setSetterName(element.getName());

      if (recordDefinition.isChoice() && modelGroup.getForeignAttribute(ShaConstants.SHACON_KR_XSD, "setter") != null) {
        subDefinition.setSetterName(modelGroup.getForeignAttribute(ShaConstants.SHACON_KR_XSD, "setter"));
      }

      for (Evaluator<RecordDefinition, XSParticle> e : evaluators.subRecordsEvaluators()) {
        e.eval(subDefinition, particle);
      }

      XSComplexType complexType = schemas.getComplexType(element.getType().getTargetNamespace(), element.getType()
        .getName());
      for (Evaluator<RecordDefinition, XSComplexType> e : evaluators.typeEvaluators()) {
        e.eval(subDefinition, complexType);
      }

      complexType.getContentType().visit(new Visitor(evaluators, mainSchema, schemas, subDefinition));
    }
  }

  private void ensureNotPuttingPropertiesAfterSubRecords(XSElementDecl element) {
    if (numberOfSubRecordsFound > 0) {
      throw new IllegalArgumentException(
        String.format("Mixing records and properties is not supported: records have to go after properties (current element name: %s, type: %s)",
          element.getName(), element.getType().getName()));
    }
  }

  @Override
  public void modelGroup(XSModelGroup xsmodelgroup) {
    if (XSModelGroup.CHOICE.equals(xsmodelgroup.getCompositor())) {
      this.modelGroup = xsmodelgroup;
    }

    for (XSParticle part : xsmodelgroup.getChildren()) {
      particle(part);
    }
  }

  @Override
  public void particle(XSParticle xsparticle) {
    this.particle = xsparticle;
    particle.getTerm().visit(this);
  }

}
