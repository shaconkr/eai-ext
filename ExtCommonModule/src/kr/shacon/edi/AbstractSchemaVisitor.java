package kr.shacon.edi;

import com.sun.xml.xsom.*;
import com.sun.xml.xsom.visitor.XSTermVisitor;
import com.sun.xml.xsom.visitor.XSVisitor;

abstract class AbstractSchemaVisitor implements XSTermVisitor, XSVisitor {

  @Override
  public void annotation(XSAnnotation ann) {
  }

  @Override
  public void attGroupDecl(XSAttGroupDecl decl) {
  }

  @Override
  public void attributeDecl(XSAttributeDecl decl) {
  }

  @Override
  public void attributeUse(XSAttributeUse use) {
  }

  @Override
  public void complexType(XSComplexType type) {
  }

  @Override
  public void schema(XSSchema schema) {
  }

  @Override
  public void facet(XSFacet facet) {
  }

  @Override
  public void notation(XSNotation notation) {
  }

  @Override
  public void identityConstraint(XSIdentityConstraint decl) {
  }

  @Override
  public void xpath(XSXPath xp) {
  }

  @Override
  public void simpleType(XSSimpleType simpleType) {
  }

  @Override
  public void particle(XSParticle particle) {
  }

  @Override
  public void empty(XSContentType empty) {
  }

  @Override
  public void wildcard(XSWildcard wc) {
  }

  @Override
  public void modelGroupDecl(XSModelGroupDecl decl) {
  }

  @Override
  public void modelGroup(XSModelGroup group) {
  }

  @Override
  public void elementDecl(XSElementDecl decl) {
  }

}
