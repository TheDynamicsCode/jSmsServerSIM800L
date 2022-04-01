/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.setting;

import biz.szydlowski.sms.api.CommandApi;
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
 * @author szydlowskidom
 */
public class CommandsMaps  {
  
   private List<CommandApi> commandsApi = new ArrayList<>();
    
   private String _setting="setting/commands-mapping.xml";
    
  static final Logger logger =  LogManager.getLogger("biz.szydlowski.sms.setting.CommandMaps");

   
     public CommandsMaps (){
         
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
             
		logger.debug("Read commands-mapping " + _setting);
                
                
                NodeList  nList = doc.getElementsByTagName("map");
                		 
		for (int temp = 0; temp < nList.getLength(); temp++) {
                
		   Node nNode = nList.item(temp);
		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                          Element eElement = (Element) nNode;
                          
                          CommandApi _commandApi = new CommandApi();
                          _commandApi.setGsmAlias(getTagValue("gsm_alias", eElement));
                          _commandApi.setCommand(getTagValue("command", eElement));
                          
                           if (eElement.getElementsByTagName("command").item(0).hasAttributes()){
                                    NamedNodeMap  baseElmnt_attr = eElement.getElementsByTagName("command").item(0).getAttributes();
                                    for (int i = 0; i <  baseElmnt_attr.getLength(); ++i)
                                    {
                                        Node attr =  baseElmnt_attr.item(i);

                                        if (attr.getNodeName().equalsIgnoreCase("response")){
                                            _commandApi.setResponse(attr.getNodeValue());
                                        } else if (attr.getNodeName().equalsIgnoreCase("response_error")){
                                            _commandApi.setResponse_error(attr.getNodeValue());
                                        } 

                                    }
                              } 
                          
                           for (int count = 0; count < eElement.getElementsByTagName("command").getLength(); count++) {

                              if (eElement.getElementsByTagName("command").item(count).hasAttributes()){
                                    NamedNodeMap  baseElmnt_attr = eElement.getElementsByTagName("command").item(count).getAttributes();
                                    String username="";
                                    
                                    for (int i = 0; i <  baseElmnt_attr.getLength(); ++i)
                                    {
                                        Node attr =  baseElmnt_attr.item(i);

                                        if (attr.getNodeName().equalsIgnoreCase("username")){
                                            logger.debug(attr.getNodeValue() + " = " + eElement.getElementsByTagName("command").item(count).getTextContent());
                                            _commandApi.setCommandMapForUser(attr.getNodeValue(), eElement.getElementsByTagName("command").item(count).getTextContent());
                                             username = attr.getNodeValue();
                                        } 

                                    }
                                    if (username.length()>0){
                                        for (int i = 0; i <  baseElmnt_attr.getLength(); ++i){
                                           Node attr =  baseElmnt_attr.item(i);

                                           if (attr.getNodeName().equalsIgnoreCase("response")){
                                                    logger.debug("response " + username + " = " + attr.getNodeValue());
                                                   _commandApi.setResponseForUser( username, attr.getNodeValue());
                                            } else if (attr.getNodeName().equalsIgnoreCase("response_error")){
                                                    logger.debug("response_error " + username + " = " + attr.getNodeValue());
                                                   _commandApi.setResponseErrorForUser(username, attr.getNodeValue());
                                            }


                                        }
                                    }
                              } 
                           }
                         
                         
                          
                          _commandApi.setGroups(getTagValue("groups", eElement));
                          _commandApi.setLoginRequired(getTagValue("loginRequired", eElement));
                          
                         commandsApi.add(_commandApi);
		   }
		}
                
                logger.debug("Read commands-mapping done");
                                
         }  catch (ParserConfigurationException | IOException e) {         
                logger.fatal("commands-mapping.xml::XML Exception/Error:", e);
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
  

  public List<CommandApi> getCommandsApi(){
          return  commandsApi;
  }
   

}