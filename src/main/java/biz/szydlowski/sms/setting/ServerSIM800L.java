package biz.szydlowski.sms.setting;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import biz.szydlowski.sms.api.ModemApi;
import biz.szydlowski.sms.api.ServerApi;
import biz.szydlowski.sms.api.ValidatorApi;
import biz.szydlowski.utils.OSValidator;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 *
 * @author szydlowskidom
 */
public class ServerSIM800L {
  
   private ServerApi serverApi = new ServerApi();
   private List<String> apiConn = new ArrayList<>(); 
   private List<String> adminConn = new ArrayList<>(); 
   private List<String> startingCommand = new ArrayList<>();
   private ValidatorApi validatorApi  = new ValidatorApi(); 
   private ModemApi modemApi = new ModemApi();
   
   private String _setting="setting/serverSIM800L.properties";
    
  static final Logger logger =  LogManager.getLogger(ServerSIM800L.class);

       
     /** Konstruktor pobierający parametry z pliku konfiguracyjnego "config.xml"
     */
     public  ServerSIM800L (){
         
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
              _setting = absolutePath + "/" + _setting;
         }

	InputStream input = null;

	try {

		input = new FileInputStream(_setting );
                Properties prop = new Properties();
		// load a properties file
		prop.load(input);
                
                logger.info("Read serverSIM800L.properties");
                           
                serverApi.setRestartScript(prop.getProperty("restart.script", "restart"));
                serverApi.setWebPort(prop.getProperty("web.port", "8080"));
                serverApi.setSmsPackageExpireDate(prop.getProperty("sms.package.expire.date", "2017-01-01"));
                serverApi.setQuartzEnabled(prop.getProperty("quartz.enabled", "true"));
                serverApi.setMaxInboundMsgWeb(prop.getProperty("web.max.inbound.msg", "20"));
                serverApi.setMaxOutboundMsgWeb(prop.getProperty("web.max.outbound.msg", "20"));
                serverApi.setWebMaxConnectionCount(prop.getProperty("web.max.connection.count", "20"));
                serverApi.setAtRretryCount(prop.getProperty("SIM800L.retry.count", "4"));  
                serverApi.setApiKey(prop.getProperty("web.apiKey", "5546644")); 
                serverApi.setAtErrorThreshold(prop.getProperty("SIM800L.error.threshold", "5"));
                serverApi.setMaxSmsInHour(prop.getProperty("SIM800L.max.sms.hour", "5"));
                
                modemApi.setPortNameMaster(prop.getProperty("SIM800L.port.name.master", "default"));
                modemApi.setPortNameFirstSlave(prop.getProperty("SIM800L.port.name.slave.1", "default"));
                modemApi.setPortNameSecondSlave(prop.getProperty("SIM800L.port.name.slave.2", "default"));                
                modemApi.setPortTimeout(prop.getProperty("SIM800L.port.timeout", "2000"));
                modemApi.setPortDataRate(prop.getProperty("SIM800L.port.data.rate", "9600"));
                modemApi.setAT_CSCA(prop.getProperty("SIM800L.CSCA", "+48790998250"));
                
                
                validatorApi.setFormPhoneMaxLength(prop.getProperty("form.phone.maxLength", "10"));
                validatorApi.setFormPhoneMinLength(prop.getProperty("form.phone.minLength", "1"));
                validatorApi.setFormPhoneStartsWith(prop.getProperty("form.phone.startsWith", "+"));
               
              
                Set<Object> keys = getAllKeys( prop );
                keys.stream().map((k) -> (String)k).forEachOrdered((key) -> {
                    if ( getPropertyValue(prop , key).length()==0){ 
                    } else if (key.startsWith("web.api.host")) {
                        apiConn.add(getPropertyValue(prop, key));
                    } else if (key.startsWith("web.admin.host")) {
                        adminConn.add(getPropertyValue(prop, key));
                    } else if (key.startsWith("start")) {
                       // logger.info("start " + getPropertyValue(prop, key));
                        startingCommand.add(getPropertyValue(prop, key));
                    }
                }); 
                
                prop.clear();
    
	} catch (IOException ex) {
		logger.error(ex);
	} finally {
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}

  
    }


  public ServerApi getServerApi (){
       return   serverApi;
  }   
  
   public  List<String> getAPIConn (){
          return  apiConn;
   } 
   
   public  List<String> getAdminConn (){
          return  adminConn;
   }
  
    private Set<Object> getAllKeys(Properties prop){
            Set<Object> keys = prop.keySet();
            return keys;
    }

    private String getPropertyValue(Properties prop, String key){
            return prop.getProperty(key);
    }  
    
   public ModemApi getModemApi (){
          return modemApi;
   } 
   
   public  List<String> getStartingCommand (){
          return  startingCommand;
   } 
  
   public ValidatorApi getValidatorApi (){
          return  validatorApi;
   }
           
   

}
