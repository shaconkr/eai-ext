package kr.shacon.edi;

import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.parser.JAXPParser;
import com.sun.xml.xsom.parser.XSOMParser;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads the record definition and creates as many {@link RecordDefinition}s as needed
 */
public class DefinitionLoader {

  private final Logger log = Logger.getLogger(DefinitionLoader.class.getName());

  private final Evaluators evaluators;
  private final RecordDefinition recordDefinition;
  private boolean loaded;

  public DefinitionLoader() {
    this.recordDefinition = new RecordDefinition();
    this.evaluators = new Evaluators();
    this.loaded = false;
  }

  /**
   * Parses the input .xsd and creates as many {@link RecordDefinition}s as needed
   *
   * @param file the XML Schema file with the file definition
   * @return the loaded record definition
   */
  public RecordDefinition load(File file) {
    try {
      return load(new InputSource(file.toURI().toURL().toExternalForm()));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Parses the input .xsd and creates as many {@link RecordDefinition}s as needed
   *
   * @param reader a reader that reads the XML Schema file with the file definition
   * @return the loaded record definition
   */
  public RecordDefinition load(Reader reader) {
    return load(new InputSource(reader));
  }

  /**
   * Parses the input .xsd and creates as many {@link RecordDefinition}s as needed
   *
   * @param input an InputSource that reads the XML Schema file with the file definition
   * @return the loaded record definition
   */
  public RecordDefinition load(InputSource input) {
    if (loaded) {
      throw new IllegalStateException("Already loaded");
    }

    log.log(Level.FINE, "Loading record definition");

    XSSchemaSet schemas = loadSchemas(input);
    XSSchema mainSchema = findMainSchema(schemas);
    XSElementDecl mainElement = mainSchema.getElementDecl("root");

    for (Evaluator<RecordDefinition, XSElementDecl> e : evaluators.mainElementEvaluators()) {
      e.eval(recordDefinition, mainElement);
    }

    XSComplexType mainRecordType = mainElement.getType().asComplexType();
    for (Evaluator<RecordDefinition, XSComplexType> e : evaluators.typeEvaluators()) {
      e.eval(recordDefinition, mainRecordType);
    }

    mainRecordType.getContentType().visit(new Visitor(evaluators, mainSchema, schemas, recordDefinition));

    this.loaded = true;

    return recordDefinition;
  }

  private XSSchemaSet loadSchemas(InputSource input) {
    XSOMParser parser = new XSOMParser(new JAXPParser(SAXParserFactory.newInstance()));
    parser.setErrorHandler(new ErrorHandler() {

      public void error(SAXParseException exception) {
        throw new RuntimeException(exception);
      }

      public void fatalError(SAXParseException exception) {
        throw new RuntimeException(exception);
      }

      public void warning(SAXParseException exception) {
        throw new RuntimeException(exception);
      }

    });

    try {
      parser.parse(input);

      return parser.getResult();
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }
  }

  private XSSchema findMainSchema(XSSchemaSet schemas) {
    List<XSSchema> schemasWithMainElement = new ArrayList<>();
    for (XSSchema schema : schemas.getSchemas()) {
      XSElementDecl main = schema.getElementDecl("root");
      if (main != null) {
        schemasWithMainElement.add(schema);
      }
    }

    if (schemasWithMainElement.isEmpty()) {
      throw new IllegalArgumentException("No \"main\" element found");
    }

    if (schemasWithMainElement.size() != 1) {
      throw new IllegalArgumentException("Multiple \"main\" elements found in your schemas");
    }

    return schemasWithMainElement.get(0);
  }

}
