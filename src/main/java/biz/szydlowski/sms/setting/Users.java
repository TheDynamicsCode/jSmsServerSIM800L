/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.setting;

import biz.szydlowski.sms.api.UserApi;
import biz.szydlowski.sms.api.UserPhonebook;
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
 * @author dominik
 */
public class Users {
  
   private final List<UserApi> usersApi = new ArrayList<>();
   private final List<UserPhonebook> usersPhonebook = new ArrayList<>();
    
   private String _setting_api="setting/users.xml";
   private String _setting_phonebook="setting/phonebook.xml";
    
   static final Logger logger =  LogManager.getLogger("biz.szydlowski.sms.setting.Users");

   
    public Users (){
         
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
              _setting_api = absolutePath + "/" + _setting_api;
         }  
         
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
             _setting_phonebook = absolutePath + "/" + _setting_phonebook;
         }

        setAPIUsers();
        setPhonebookUsers();
          
       
   }
    
  private void setAPIUsers(){
        try {
                        
		File fXmlFile = new File(_setting_api);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = (Document) dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
             
		logger.debug("Read API user " + _setting_api);
                
                
                NodeList  nList = doc.getElementsByTagName("user");
                		 
		for (int temp = 0; temp < nList.getLength(); temp++) {
                
		   Node nNode = nList.item(temp);
		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                          Element eElement = (Element) nNode;
                          
                          UserApi userApi = new UserApi();
                          UserPhonebook userPhonebook = new UserPhonebook();
                           
                          userApi.setUsername(getTagValue("username", eElement));
                          userApi.setPhone(getTagValue("phone", eElement));
                          userApi.setAdmin(getTagValue("admin", eElement));
                          userApi.setActiveAtStartup(getTagValue("activeAtStartup", eElement));
                          userApi.setRedirectUnknownSms(getTagValue("redirectUnknownSms", eElement));
                          userApi.setGroups(getTagValue("groups", eElement));
                          userApi.setTimeoutInMinutes(getTagValue("timeout_minutes", eElement));
                          
                          userPhonebook.setUsername(getTagValue("username", eElement));
                          userPhonebook.setPhone(getTagValue("phone", eElement));
                          userPhonebook.setGroups(getTagValue("groups", eElement));
                          
                          usersApi.add(userApi);
                          usersPhonebook.add(userPhonebook);
		   }
		}
                
                logger.debug("Read API user done");
                                
         }  catch (ParserConfigurationException | IOException e) {         
                logger.fatal("users.xml::XML Exception/Error:", e);
                System.exit(-1);
				
	  } catch (SAXException ex) {
             logger.error(ex);
       }
  }  
  
  private void setPhonebookUsers(){
        try {
                        
		File fXmlFile = new File(_setting_phonebook);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = (Document) dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
             
		logger.debug("Read Phonebook user " + _setting_phonebook);
                
                
                NodeList  nList = doc.getElementsByTagName("contact");
                		 
		for (int temp = 0; temp < nList.getLength(); temp++) {
                
		   Node nNode = nList.item(temp);
		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                          Element eElement = (Element) nNode;
                        
                          UserPhonebook userPhonebook = new UserPhonebook();
                     
                          userPhonebook.setUsername(getTagValue("username", eElement));
                          userPhonebook.setPhone(getTagValue("phone", eElement));
                          userPhonebook.setGroups(getTagValue("groups", eElement));
                        
                          usersPhonebook.add(userPhonebook);
		   }
		}
                
                logger.debug("Read Phonebook user done");
                                
         }  catch (ParserConfigurationException | IOException e) {         
                logger.fatal("users.xml::XML Exception/Error:", e);
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
  

  public List<UserApi> getUsersApi(){
          return usersApi;
  } 
  
  public List<UserPhonebook> getUsersPhonebook(){
         return usersPhonebook;
  }
   

}

