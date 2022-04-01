/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.setting;

import biz.szydlowski.sms.api.AutoLogCronApi;
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Dominik
 */
public class AutoLogCrontab   {

   private List<AutoLogCronApi> AutoLogCronsApi   = new ArrayList<>();
    
   private String _setting="setting/autolog-crontab.xml";
    
   static final Logger logger =  LogManager.getLogger(AutoLogCrontab.class);

   
     public AutoLogCrontab(){
         
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
              _setting = absolutePath + "/" + _setting;
         }

     
          
         try {
                        
		File fXmlFile = new File(_setting);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = (Document) dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
             
		logger.debug("Read autolog-crontab " + _setting);
                
                
                NodeList  nList = doc.getElementsByTagName("crontab");
                		 
		for (int temp = 0; temp < nList.getLength(); temp++) {
                
		   Node nNode = nList.item(temp);
		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                                              
                          Element eElement = (Element) nNode;
                         
                          AutoLogCronApi _autoLogCron = new  AutoLogCronApi ();
                                     
                          String cron_expression = getTagValue("cronExpression", eElement);
                         
                          if (org.quartz.CronExpression.isValidExpression(cron_expression)){
                              _autoLogCron.setCronExpression(cron_expression);
                          } else {
                              _autoLogCron.setCronExpression("0 0 0 * * ?");
                               logger.error("Invalid cron expression : " +   cron_expression);
                          }
                         
                          if (eElement.getElementsByTagName("usersOrGroups").item(0).hasAttributes()){
                                NamedNodeMap  baseElmnt_attr = eElement.getElementsByTagName("usersOrGroups").item(0).getAttributes();
                                for (int i = 0; i <  baseElmnt_attr.getLength(); ++i) {
                                    Node attr =  baseElmnt_attr.item(i);
                                    logger.debug(attr.getNodeName() + " = " + attr.getNodeValue());
                                    if (attr.getNodeName().equals("type")){
                                        if (attr.getNodeValue().equals("user")){
                                            _autoLogCron.setIsUser();
                                        } else if (attr.getNodeValue().equals("group")){
                                            _autoLogCron.setIsGroup();
                                        }
                                    }
                                    
                                }
                            } else {
                                logger.warn("usersOrGroups has not attributes !!!!");
                            }  
                          
                          _autoLogCron.setAlias(getTagValue("alias", eElement));
                          
                          _autoLogCron.setActionType(getTagValue("actionType", eElement));
                          _autoLogCron.setUsersOrGroups(getTagValue("usersOrGroups", eElement));
                          
                          AutoLogCronsApi.add(_autoLogCron);
                        
		   }
		}
                
                logger.debug("Read autolog-crontab done");
                                
         }  catch (ParserConfigurationException | IOException e) {         
                logger.fatal("autolog-crontab .xml::XML Exception/Error:", e);
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
  

  public List<AutoLogCronApi> getAutoLogCronsApi(){
        return  AutoLogCronsApi;
  }
  
}
