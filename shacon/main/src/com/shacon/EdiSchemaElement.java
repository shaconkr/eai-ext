package com.shacon;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class EdiSchemaElement extends XmlSchemaElement {

    String length;

    public EdiSchemaElement(XmlSchema parentSchema, boolean topLevel) {
        super(parentSchema, topLevel);
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }
}
