package com.hcis.eai.ext;

import kr.shacon.edi.Transformer;

import java.io.IOException;
import java.io.Serializable;


public class EDITransformer implements Serializable {

	private static final long serialVersionUID = 6361558241196592638L;

	protected String xsdFile;
	protected String jsonString;
	protected String ediString;

	Transformer transformer;

	public EDITransformer() {
		transformer = new Transformer();
	}


	public void toEDIString() throws IOException {
		setEdiString(transformer.toEDI(getXsdFile(), getJsonString()));
	}

	public  void toJSONString() throws IOException {
		setJsonString(transformer.toJSON(getXsdFile(), getEdiString()));
	}


	public String getXsdFile() {
		return xsdFile;
	}

	public String getJsonString() {
		return jsonString;
	}

	public void setJsonString(String jsonString) {
		this.jsonString = jsonString;
	}

	public String getEdiString() {
		return ediString;
	}

	public void setEdiString(String ediString) {
		this.ediString = ediString;
	}


}
