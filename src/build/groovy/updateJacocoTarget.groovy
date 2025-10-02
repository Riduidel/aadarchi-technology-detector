import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import groovy.xml.*
import groovy.xml.dom.*

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

def PROPERTY = "jacoco.covered.instructions.target.percentage"

File jacocoReport = new File(basedir, "/target/site/jacoco/index.html")
log.info "Reading effective coverage from ${jacocoReport.absolutePath}"
def index = Jsoup.parse(jacocoReport, "UTF-8", "http://example.com")
def effectiveJacocoLevelElement = index.select("tfoot tr td.ctr2").first().text()
def effectiveJacocoLevelText = effectiveJacocoLevelElement.substring(0, effectiveJacocoLevelElement.indexOf('%'))
// It's a percentage!
def effectiveJacocoLevel = Double.parseDouble(effectiveJacocoLevelText)/100
def initialJacocoLevelText = properties['jacoco.covered.instructions.target.percentage']
initialJacocoLevelText = initialJacocoLevelText?:"0"
def initialJacocoLevel = Double.parseDouble(initialJacocoLevelText)
log.info "Required coverage is ${initialJacocoLevel*100}%. Mesured coverage is ${effectiveJacocoLevel*100}%"
if(effectiveJacocoLevel>initialJacocoLevel) {
	log.info "We improved the coverage! Let's assume that new quality level!"
	def pomFile = new File(basedir, "pom.xml")
	def doc = DOMBuilder.newInstance().parseText(pomFile.text)
	def projectXml = doc.documentElement
	use(DOMCategory) {
		def groupId = projectXml.groupId.isEmpty() ? projectXml.parent[0].groupId[0].text() : projectXml.groupId[0].text()
		assert groupId!=null
		def artifactId = projectXml.artifactId[0].text() 
		assert artifactId!=null
		def coordinates = "${groupId}:${artifactId}"
		log.info "Parsed POM of ${coordinates}"
		def properties = projectXml.children().find { node -> node.name()=="properties"}
		if(properties==null) {
			log.warn "There are no <properties/> defined in ${coordinates}. Appending them"
			projectXml.appendChild(doc.createElement("properties"))
			properties = projectXml.children().find { node -> node.name()=="properties"}
		}
		// Do we have the good property in?
		def maybeJacocoProperty = properties.findAll { node -> node.name()==PROPERTY }
		if(maybeJacocoProperty.size()==0) {
			log.warn "There is no <${PROPERTY}/> defined in <properties/> of ${coordinates}. Appending it"
			def jacocoPropertyNode = doc.createElement(PROPERTY)
			jacocoPropertyNode.appendChild(doc.createTextNode("0"))
			properties.appendChild(jacocoPropertyNode)
		}
		def jacocoProperty = properties.findAll { node -> node.name()==PROPERTY }.first()
		def jacocoStringValue = String.format(Locale.US, "%1.2f", effectiveJacocoLevel)
		jacocoProperty.value = jacocoStringValue
		log.info "Updated <${PROPERTY}/> value to ${jacocoStringValue}"


		// Thanks Baeldung :-( https://www.baeldung.com/java-write-xml-document-file
		TransformerFactory transformerFactory = TransformerFactory.newInstance()
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		FileWriter writer = new FileWriter(pomFile);
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
		// def printer = new XmlNodePrinter(new PrintWriter(new FileWriter(pomFile)))
		// printer.preserveWhitespace = false
		// printer.print(projectXml)
		log.info "New coverage target has been written to ${pomFile}"
	}
}
