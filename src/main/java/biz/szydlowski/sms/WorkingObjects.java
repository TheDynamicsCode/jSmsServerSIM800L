/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms;

import static biz.szydlowski.sms.JDaemonSms.functions;
import static biz.szydlowski.sms.JDaemonSms.logger;
import biz.szydlowski.sms.api.CommandApi;
import biz.szydlowski.sms.api.ModemApi;
import biz.szydlowski.sms.api.ServerApi;
import biz.szydlowski.sms.api.SmsTepmlateApi;
import biz.szydlowski.sms.api.UserApi;
import biz.szydlowski.sms.api.UserPhonebook;
import biz.szydlowski.sms.api.ValidatorApi;
import biz.szydlowski.sms.setting.CommandsMaps;
import biz.szydlowski.sms.setting.ServerSIM800L;
import biz.szydlowski.sms.setting.SmsTemplate;
import biz.szydlowski.sms.setting.Users;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.util.List;


/**
 *
 * @author Dominik
 */
public class WorkingObjects {
 
     public static List<String> apiConn = null;
     public static List<String> adminConn = null;
     public static List<String> smsCommands = null;
     public static List<String> uniquePhonebook_groupName = null;
     public static List<UserApi> usersApi = null;
     public static List<UserPhonebook> usersPhonebook = null;
     public static List<SmsTepmlateApi> smsTepmlatesApi = null; 
     public static ModemApi modemApi = null;   
     public static ValidatorApi validatorApi = null;
     public static ServerApi serverApi = null;
     
     public static boolean restart_watchdog=false; 
     public static boolean kill_watchdog=false;
     public static boolean warn_sms_expire_trigger=true; 
     public static boolean error_sms_expire=false; 
        
     public static  long lastSMSReader;
     public static  long lastMaintenance;
     public static  long lastUserSession;
     public static  long lastSerialRead;
     public static  long lastSerialWrite;
     public static  long lastErrors;
     public static  long lastOK;
     public static  long atErrorThreshold;
     public static boolean ioErrorDetected=false; 
     public static int smsreadertime_sec = 600;
     
     public static List<String> md5SMSHeader = null;     
     public static String TOKEN = "1234567891011";
     public static String TOKEN_OLD = "1234567891011";     
    
     static SimpleDateFormat sdfexpire = new SimpleDateFormat("yyyy-MM-dd");
     public static Date SMS_ExpireDate;
       
     public static ServerSIM800L _server;
     public static String apiKey = "";
     
     public static int RETRYCOUNT = 4;  
     public static List<CommandApi> commands_map = null;  
     
     public static ArchiveToFile archiveToFile = null;
        
     public static void init (){     
         
           if ( functions == null) {
               System.err.println("ERROR x555");
           }
         
          _server = new ServerSIM800L();           
      
          
          TOKEN = "1234567891011";
          TOKEN_OLD = "1234567891011";
          smsreadertime_sec = 600;
          ioErrorDetected=false;   
          restart_watchdog=false;
          kill_watchdog=false;
          
          apiConn = new ArrayList<>();
          adminConn = new ArrayList<>();
          smsCommands = new ArrayList<>();
          smsTepmlatesApi = new ArrayList<>();
          md5SMSHeader= new ArrayList<>();
          
          lastSMSReader = System.currentTimeMillis();
          lastMaintenance= System.currentTimeMillis();
          lastUserSession= System.currentTimeMillis();
          lastSerialRead= System.currentTimeMillis();
          lastSerialWrite= System.currentTimeMillis();
            
          lastErrors = 0;
          lastOK = 0;
          atErrorThreshold = 0;
                        
          usersApi = new Users().getUsersApi();
          usersPhonebook = new Users().getUsersPhonebook();
          commands_map = new CommandsMaps().getCommandsApi();
          
          
           uniquePhonebook_groupName =  functions.getUniqGroupName(usersPhonebook);
         
          
           serverApi = _server.getServerApi();
           apiConn = _server.getAPIConn();
           adminConn = _server.getAdminConn();
           
           RETRYCOUNT = serverApi.getAtRretryCount();
           apiKey = serverApi.getApiKey();
           
           try {
                SMS_ExpireDate= sdfexpire.parse(serverApi.getSmsPackageExpireDate());
           } catch (ParseException ex) {  
                ex.printStackTrace();
           }
           
           validatorApi = _server.getValidatorApi();
           
           smsTepmlatesApi = new SmsTemplate().getSmsTepmlatesApi();   
        
         
           modemApi = _server.getModemApi();
        
           archiveToFile = new ArchiveToFile();
          
           usersApi.stream().filter((user) -> (user.isActiveAtStartup())).map((user) -> {
                logger.info("Add user " + user.getUsername());
                return user;
            }).map((user) -> {
                archiveToFile.addToRepository("audit.out", "Login/SAS\t" + user.getUsername());
                return user;
            }).forEachOrdered((user) -> {
                activeUsers.addUser(user.getUsername());
            });
     }
    
         
    public static void clear(){
       // if (_SybaseQuartz!=null) _SybaseQuartz.stop(); 
        
        if (apiConn!=null) apiConn.clear();
        if (adminConn !=null) adminConn .clear();
        if (smsCommands !=null) smsCommands .clear();
        if (uniquePhonebook_groupName !=null) uniquePhonebook_groupName .clear();
     
      
        md5SMSHeader.clear();       
        usersApi.clear();
        usersPhonebook.clear();
        commands_map.clear();
        smsTepmlatesApi.clear();
     
        
    }
    
    
    public static void destroy(){
    
    
    }
    
}