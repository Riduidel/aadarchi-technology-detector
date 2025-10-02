import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import groovy.xml.*

def PROPERTY = "jacoco.covered.instructions.target.percentage"
File jacocoReport = new File(basedir, "/target/site/jacoco/index.html")
log.info "Reading effective coverage from ${jacocoReport.absolutePath}"
def index = Jsoup.parse(jacocoReport, "UTF-8", "http://example.com")
def effectiveJacocoLevelElement = index.select("tfoot tr td.ctr2").first().text()
def effectiveJacocoLevelText = effectiveJacocoLevelElement.substring(0, effectiveJacocoLevelElement.indexOf('%'))
// It's a percentage!
def effectiveJacocoLevel = Double.parseDouble(effectiveJacocoLevelText)/100
def initialJacocoLevel = Double.parseDouble(properties['jacoco.covered.instructions.target.percentage'])
log.info "Required coverage is ${initialJacocoLevel*100}%. Mesured coverage is ${effectiveJacocoLevel*100}%"
if(effectiveJacocoLevel>initialJacocoLevel) {
	log.info "We improved the coverage! Let's assume that new quality level!"
	def pomFile = new File(basedir, "pom.xml")
	def projectXml = new XmlParser().parse(pomFile)
	def groupId = projectXml.groupId.isEmpty() ? projectXml.parent[0].groupId[0].text() : projectXml.groupId[0].text()
	def artifactId = projectXml.artifactId[0].text() 
	def coordinates = "${groupId}:${artifactId}"
	log.info "Parsed POM of ${coordinates}"
	if(projectXml.properties.size()==0) {
		log.warn "There are no <properties/> defined in ${coordinates}. Appending them"
		projectXml.append(new NodeBuilder().properties())
	}
	def properties = projectXml.properties[0]
	// Do we have the good property in?
	def maybeJacocoProperty = properties.findAll { PROPERTY.equals(it.name().localPart) }
	if(maybeJacocoProperty.size()==0) {
		log.warn "There is no <${PROPERTY}/> defined in <properties/> of ${coordinates}. Appending it"
		properties.append(new NodeBuilder()."${PROPERTY}"())
	}
	def jacocoProperty = properties.findAll { PROPERTY.equals(it.name().localPart) }.first()
	def jacocoStringValue = String.format(Locale.US, "%1.2f", effectiveJacocoLevel)
	jacocoProperty.value = jacocoStringValue
	log.info "Updated <${PROPERTY}/> value to ${jacocoStringValue}"

	jacocoProperty.value = String.format(Locale.US, "%1.2f", effectiveJacocoLevel)
	def printer = new XmlNodePrinter(new PrintWriter(new FileWriter(pomFile)))
	printer.preserveWhitespace = true
	printer.print(projectXml)
	log.info "New coverage target has been written to ${pomFile}"
}
