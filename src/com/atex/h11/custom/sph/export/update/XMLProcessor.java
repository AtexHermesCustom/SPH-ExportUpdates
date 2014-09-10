package com.atex.h11.custom.sph.export.update;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLProcessor {
    private static final String loggerName = XMLProcessor.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);   
    
    private Properties props = null;
    private Document doc = null;

    private DocumentBuilderFactory docBuilderFactory = null;
    private DocumentBuilder docBuilder = null;      
    private XPathFactory xpFactory = null;
    private XPath xp = null;    
    private TransformerFactory transFactory = null;
    private Transformer trans = null;    
    
    private String j2eeIP = null;
    private String j2eeHttpPort = null;
    
    private String[] nonPaginatedTypes;
    private String outputFileName = null;
    
    public XMLProcessor(Properties props, File inputFile) 
            throws ParserConfigurationException, SAXException, IOException, TransformerConfigurationException {
        logger.entering(loggerName, "XMLProcessor");
        
        this.props = props;
        
        j2eeIP = System.getenv("J2EE_IP");
        j2eeHttpPort = System.getenv("J2EE_HTTPPORT");
        
        // Prepare a transfomer.
        transFactory = TransformerFactory.newInstance();        
        trans = transFactory.newTransformer();
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        trans.setOutputProperty(OutputKeys.METHOD, "xml");
        trans.setOutputProperty(OutputKeys.STANDALONE, "yes");
        trans.setOutputProperty("{http://xml.apache.org/xsl}indent-amount", "4");
        
        xpFactory = XPathFactory.newInstance();
        xp = xpFactory.newXPath();           
        
        // Prepare a document builder.
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        docBuilder = docBuilderFactory.newDocumentBuilder();     
        
        // parse the input xml file
        logger.info("Processing file: " + inputFile.getCanonicalPath());
        doc = docBuilder.parse(inputFile);
        outputFileName = inputFile.getName();
        
        logger.exiting(loggerName, "XMLProcessor");
    }
    
    public void Start() 
            throws XPathExpressionException, TransformerException, MalformedURLException, IOException, SAXException {
        logger.entering(loggerName, "Start");
        
        // Non paginated object types
        if (! props.getProperty("nonPaginatedTypesToInclude").trim().isEmpty()) {
            nonPaginatedTypes = props.getProperty("nonPaginatedTypesToInclude").split(",");

            for (int i = 0; i < nonPaginatedTypes.length; i++) {
                logger.info("Non-paginated object type to include: " + nonPaginatedTypes[i]); 
            }

            // look for all story packages
            NodeList nl = (NodeList) xp.evaluate("//ncm-object[ncm-type-property/object-type/@id=17]", 
                    doc, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                CheckNonPaginatedElems(nl.item(i));
            }
        }
        else {
            logger.info("No non-paginated object types to include.");
        }
        
        // output
        DOMSource source = new DOMSource(doc);
        File outputFile = new File(props.getProperty("destinationDir"), outputFileName);
        StreamResult result = new StreamResult(outputFile);
        trans.transform(source, result);        
        logger.info("Saved file to: " + outputFile.getCanonicalPath());
        
        logger.exiting(loggerName, "Start");
    }
    
    private void CheckNonPaginatedElems(Node spNode)
            throws XPathExpressionException, TransformerException, MalformedURLException, IOException, SAXException {
        logger.entering(loggerName, "CheckNonPaginatedElems");        
        
        URL url = null;
        HttpURLConnection conn = null;        
                
        try {
            String spName = xp.evaluate("name", spNode);
            String spId = xp.evaluate("obj_id", spNode);
            logger.info("Checking SP name=" + spName + ", objid=" + spId);

            String urlStr = props.getProperty("ncmInputURL");
            urlStr = urlStr.replace("$J2EE_IP", j2eeIP);
            urlStr = urlStr.replace("$J2EE_HTTPPORT", j2eeHttpPort);
            urlStr = urlStr.replace("$SP_ID", spId);
            logger.fine("Connecting to: " + urlStr);
            url = new URL(urlStr);

            // Connect
            conn = (HttpURLConnection) url.openConnection();       
            conn.setDoOutput(false);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            // parse xml result
            Document spDoc = docBuilder.parse(conn.getInputStream());
            logger.fine("XML for SP retrieved and parsed");

            // look for non paginated object types
            for (int i = 0; i < nonPaginatedTypes.length; i++) {
                String objType = nonPaginatedTypes[i];
                NodeList nl = 
                    (NodeList) xp.evaluate("//child-of-a-ncm-sp-object/ncm-object[ncm-type-property/object-type/@id=" + objType + "]", 
                        spDoc, XPathConstants.NODESET);
                for (int j = 0; j < nl.getLength(); j++) {
                    Node n = nl.item(j);
                    String objName = xp.evaluate("name", n);
                    String objId = xp.evaluate("obj_id", n);
                    logger.fine("Found object: name=" + objName + ", id=" + objId + ", type=" + objType);
                    
                    // include this node to the sp's parent logical page
                    Node adoptNode = doc.adoptNode(n.cloneNode(true));
                    spNode.getParentNode().getParentNode().getParentNode().appendChild(adoptNode);
                    logger.fine("Included object: name=" + objName + ", id=" + objId + ", type=" + objType);
                }                
            }   
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "", e);
        }
        finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        
        logger.exiting(loggerName, "CheckNonPaginatedElems");     
    }
}
