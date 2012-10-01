/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 4/7/11
 * Time: 9:27 AM
 */
package siris.core.helper

import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.{InputSource, XMLReader}
import xml.parsing.NoBindingFactoryAdapter
import xml.{MetaData, TopScope, Elem}
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{SchemaFactory, Schema, ValidatorHandler}
import javax.xml.XMLConstants
import java.io.{FileInputStream, File}


/**
 * From "Sean Wellington's Blog"
 * Url "http://sean8223.blogspot.com/2009/09/xsd-validation-in-scala.html"
 */
class SchemaAwareFactoryAdapter(schema:Schema) extends NoBindingFactoryAdapter {

  def loadXML(source: InputSource): Elem = {
    // create parser
    val parser: SAXParser = try {
      val f = SAXParserFactory.newInstance()
      f.setNamespaceAware(true)
      f.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
      f.newSAXParser()
    } catch {
      case e: Exception =>
        Console.err.println("error: Unable to instantiate parser")
        throw e
    }

    val xr = parser.getXMLReader()
    val vh = schema.newValidatorHandler()
    vh.setContentHandler(this)
    xr.setContentHandler(vh)

    // parse file
    scopeStack.push(TopScope)
    xr.parse(source)
    scopeStack.pop
    return rootElem.asInstanceOf[Elem]
  }
}

object SchemaAwareXML {

  def loadFile(f: File): Elem = {
    val root = xml.XML.loadFile(f)
    var xsd: Option[MetaData] = root.attributes.find(_.key == "noNamespaceSchemaLocation").orElse(root.attributes.find(_.key == "schemaLocation"))

    if(!xsd.isDefined)
      root
    else {
      var xsdFile = new File(xsd.get.value.toString)
      if (!xsdFile.exists) xsdFile = new File(f.getParent, xsd.get.value.toString)

      val sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      val s = sf.newSchema(new StreamSource(xsdFile))
      val is = new InputSource(new FileInputStream(f))
      new SchemaAwareFactoryAdapter(s).loadXML(is)
      root
    }
  }
}