/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.setting;

import biz.szydlowski.sms.api.SmsTepmlateApi;
import biz.szydlowski.utils.OSValidator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Dominik
 */
public class SmsTemplate  {
  
   private final List<SmsTepmlateApi> smsTepmlatesApi = new ArrayList<>();
    
   private String _setting="setting/sms-template.xml";
    
   static final Logger logger =  LogManager.getLogger("biz.szydlowski.sms.setting.SmsTemplate");

   
    public SmsTemplate  (){
         
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
              _setting = absolutePath + "/" + _setting;
         }  
         
        setTemplateApi();
          
       
   }
    
  private void setTemplateApi(){
        try {
                        
		File fXmlFile = new File(_setting);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = (Document) dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
             
		logger.debug("Read Template user " + _setting);
                
                
                NodeList  nList = doc.getElementsByTagName("template");
                		 
		for (int temp = 0; temp < nList.getLength(); temp++) {
                
		   Node nNode = nList.item(temp);
		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                          Element eElement = (Element) nNode;
                          
                          SmsTepmlateApi smsTepmlateApi = new SmsTepmlateApi();
                       
                          smsTepmlateApi.setName(getTagValue("name", eElement));
                          smsTepmlateApi.setMessage(getTagValue("message", eElement));
                          smsTepmlateApi.setGroups(getTagValue("groups", eElement));
                          
                          smsTepmlatesApi.add(smsTepmlateApi);
		   }
		}
                
                logger.debug("Read Template user done");
                                
         }  catch (ParserConfigurationException | IOException e) {         
                logger.fatal("sms-template.xml::XML Exception/Error:", e);
                System.exit(-1);
				
	  } catch (SAXException ex) {
             logger.error(ex);
       }
  }  

  
  private static String getTagValue(String sTag, Element eElement) {
        try {
            NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
            Node nValue = (Node) nlList.item(0);
            return nValue.getNodeValue();
        } catch (Exception e){
            logger.error("getTagValue error " + sTag + " " + e);
            return "ERROR";
        }
  }
  

  public List<SmsTepmlateApi> getSmsTepmlatesApi(){
          return smsTepmlatesApi;
  } 
  
   

}
