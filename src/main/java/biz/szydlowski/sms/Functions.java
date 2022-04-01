/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms;

import static biz.szydlowski.sms.JDaemonSms.statistics;
import static biz.szydlowski.sms.WorkingObjects.SMS_ExpireDate;
import static biz.szydlowski.sms.WorkingObjects.archiveToFile;
import static biz.szydlowski.sms.WorkingObjects.commands_map;
import static biz.szydlowski.sms.WorkingObjects.md5SMSHeader;
import static biz.szydlowski.sms.WorkingObjects.serverApi;
import static biz.szydlowski.sms.WorkingObjects.smsCommands;
import static biz.szydlowski.sms.WorkingObjects.usersApi;
import static biz.szydlowski.sms.WorkingObjects.usersPhonebook;
import biz.szydlowski.sms.api.CommandApi;
import biz.szydlowski.sms.api.UserApi;
import biz.szydlowski.sms.api.UserPhonebook;
import biz.szydlowski.utils.OSValidator;
import static biz.szydlowski.utils.tasks.TasksWorkspace.absolutePath;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Dominik
 */
public class Functions {
       
        static final Logger logger =  LogManager.getLogger(Functions.class);
        static SimpleDateFormat ft =  new SimpleDateFormat ("H");
         
        public static boolean validTime(){
            Date dNow = new Date( );
            int hour = Integer.parseInt(ft.format(dNow));
            return hour>=7 && hour<=22;
        
       } 
             
        
        private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss"); 
        private static final SimpleDateFormat sdf_dis = new SimpleDateFormat("ddMMyyyyHHmm");
            
        private final ScriptExecutor _ScriptClass = new ScriptExecutor();
         
        public boolean isValidUserAPIForPhone(String phone){
            boolean ret = false;
            for (UserApi user : usersApi){
                if (user.getPhone().equalsIgnoreCase(phone)){
                    ret = true;
                    break;
                }
            }
            
            return ret;
       }
        
        public String getUserAPIForPhone(String _phone){
            String ret="default";
            
            for (UserApi user : usersApi){
                if (user.getPhone().equalsIgnoreCase(_phone)){
                    ret = user.getUsername();
                    break;
                }
            }
            
            return ret;
       } 
        
        
       public void smsReader(){
            logger.debug("****** SMS READ START  *****");
          
            UUID hand_uuid = Messaging.addtoMessaging("AT+CMGL=\"ALL\"\n", false, false); // no retry, no delete
     
            logger.debug("READ SMS add commands with uuid " + hand_uuid);
           
            long startRead = System.currentTimeMillis();
            boolean timeout=false;
            
            while (!Messaging.isDone(hand_uuid)&&!timeout){
               
                try {              
                   Thread.sleep(500);
                }
                catch (InterruptedException e)    {
                   Thread.currentThread().interrupt(); // restore interrupted status
                }
                long delay = System.currentTimeMillis() - startRead;
                if ( delay > 15000){
                    logger.warn("Commands with uuid " + hand_uuid + " timeout");
                    Messaging.deleteFromMessaging(hand_uuid, false, 0);
                    timeout = true;
                }

            }
            
            if (timeout){
                logger.debug("Commands with uuid " + hand_uuid + " was done with timeout");
                return;
            } else {
                logger.debug("Commands with uuid " + hand_uuid + " was done");
            }
       
            String smses = Messaging.at_return.get(hand_uuid);
            
            if (smses==null){
                logger.error("sms return null");
                Messaging.deleteFromMessaging(hand_uuid, false, 0);
                logger.debug("****** SMS READ done  *****");
                return;
            }
                        
            String sms_es [] = Messaging.at_return.get(hand_uuid).split("\n");
           
            int expectedResuls = -1;
            boolean admin_login=false;
            String admin_login_phone_number="";


            for (int j=0; j<sms_es.length; j++){
                expectedResuls = sms_es[j].indexOf("+CMGL");
                if (expectedResuls>-1){
                      
                     // logger.debug("header sms's: " + sms_es.get(j));
                      if (j<sms_es.length-1) {
                        //  logger.debug("sms's: " + sms_es.get(j+1));

                           String [] split_msg = sms_es[j].split(",");

                           if (split_msg.length==6){
                                   archiveToFile.addToRepository("sms_inbound_header.out", sms_es[j]);
                                                                                               
                                   String status = split_msg[1].replaceAll("\"", "");
                                   String from = split_msg[2].replaceAll("\"", "");
                                   from = from.replaceAll("\\s+", "");
                                   String user = getUserAPIForPhone(from);
                                   
                                   if (user.equals("default")){
                                       logger.info("user is default : " + user);
                                   }

                                   logger.debug("user: " + user);
                                   logger.debug("status: " + status);
                                   logger.debug("from: " + from);
                                  
                                   //BUG 20170302
                                   String md5header = calcMD5(sms_es[j].replaceAll("REC UNREAD", "").replaceAll("REC READ", ""));
                                                                     
                                   boolean isSMSInList = false;
                                   for (String _md5 : md5SMSHeader) {
                                       if (md5header.equals(_md5)) {
                                          isSMSInList = true;
                                          break;
                                      }
                                   }
                                 
                                   if (isSMSInList){
                                       logger.debug("SMS is in LIST....."); 
                                       continue;
                                   } else { 
                                       logger.debug("ADD SMS to LIST....."); 
                                       Messaging.addToLastInboundSms(sms_es[j]+"\t"+sms_es[j+1]);
                                       WorkingStats.smsReadCountPlus();
                                       md5SMSHeader.add(md5header);
                                   }
                                   
                                   //++++++++++++++++  
                                   
                                   try {
                                       int index = Integer.parseInt(split_msg[0].replaceAll("[^0-9]", ""));
                                        Messaging.addtoMessaging("AT+CMGD="+index+"\n", false, true); 
                                   } catch (NumberFormatException ignore){
                                       logger.error("ERROR INDX " + ignore.getMessage());
                                   }
                                
                                   if (sms_es[j].contains("REC READ")){
                                       logger.debug("PO STAREMU POMIJAM:)......");  
                                       continue;
                                   }
                              
                                   
                                   String sms = sms_es[j+1].replaceAll("OK", "").replaceAll("ERROR", "");
                                  
                                   StringBuilder archiwizer = new StringBuilder ();
                                   archiwizer.append(from).append("\t");
                                   archiwizer.append(sms);
                                   
                                   archiveToFile.addToRepository("sms_inbound_txt.out", archiwizer.toString());

                                   
                                   StringBuilder msg = new StringBuilder ();
                                   boolean isAdmin=false;

                                   if (isValidUserAPIForPhone(from)){
                                       isAdmin = isAdminUserAPIForPhone(from);
                                  
                                       String sms_commands [] = sms_es[j+1].replaceAll("\\s+", "").replaceAll("OK", "").replaceAll("ERROR", "").split(";");
                                   
                                       logger.debug("sms: " + sms);

                                       for (String sms_command : sms_commands) {
                                           if (sms_command.equalsIgnoreCase("help")) {
                                               for (String sms_c : smsCommands){
                                                   if (sms_c.startsWith("admin")){
                                                       if (isAdmin){
                                                           msg.append(sms_c);
                                                           msg.append(", ");
                                                       }
                                                   } else {
                                                       msg.append(sms_c);
                                                       msg.append(", ");
                                                   }
                                               }
                                               msg.delete(msg.lastIndexOf(","), msg.length());
                                               msg.append("\n======\n");
                                           } else if (sms_command.equalsIgnoreCase("heartbeat")) {
                                               msg.append("Hello, I'm alive !!!!");
                                           } else if (sms_command.equalsIgnoreCase("mapcommands")) {
                                               msg.append("Command maps: ");
                                               
                                               List<String> split_group_for_user  = getGroupsForUserAPI(user);
                                               
                                               for (CommandApi cmd : commands_map){
                                                   List <String> split_group_for_cmd = cmd.getGroups();
                                                   
                                                   boolean found_group = false;
                                                   
                                                   for (String sgfc : split_group_for_cmd){
                                                       for (String sgfu : split_group_for_user){
                                                           if (sgfu.equals(sgfc)){
                                                               found_group=true;
                                                               break;
                                                           }
                                                       }
                                                       if (found_group) break;
                                                   }
                                                   
                                                   if (found_group) {
                                                       msg.append(cmd.getGsmAias());
                                                       msg.append(", ");
                                                   }
                                               }
                                               msg.delete(msg.lastIndexOf(","), msg.length());
                                           } else if (sms_command.equalsIgnoreCase("version")) {
                                               msg.append("Version: ");
                                               msg.append(Version.getAgentVersion());
                                           } else if (sms_command.equalsIgnoreCase("uptime")) {
                                               msg.append("Uptime ").append(uptimeHumanReadable(WorkingStats.getUptime())).append("\n");
                                           } else if (sms_command.toLowerCase().equalsIgnoreCase("stats")) {
                                               msg.append("Sms read count ").append(WorkingStats.getSmsReadCount()).append("\n");
                                               msg.append("Sms send count ").append(WorkingStats.getSmsSendCount()).append("\n");
                                           } else if (sms_command.equalsIgnoreCase("fullstats")) {
                                               msg.append(statistics.readStatsToString());
                                           } else if (sms_command.toLowerCase().equalsIgnoreCase("starttime")) {
                                               msg.append("Startime ").append(WorkingStats.getStartTime()).append("\n");
                                           } else if (sms_command.toLowerCase().equalsIgnoreCase("smsexpiredate")) {
                                                msg.append("SMS expire date: ").append(SMS_ExpireDate).append("\n");
                                           } else if (sms_command.toLowerCase().equalsIgnoreCase("admin.getdefinedusers")) {
                                               if (isAdmin) {
                                                   msg.append("Defined users: ");
                                                   usersApi.stream().map((propuser) -> {
                                                       msg.append(propuser.getUsername());
                                                       return propuser;
                                                   }).forEachOrdered((_item) -> {
                                                       msg.append(", ");
                                                   });
                                                   msg.delete(msg.lastIndexOf(","), msg.length());
                                               } else {
                                                   msg.append("You are not a admin to execute a command ");
                                                   msg.append(sms_command);
                                               }
                                           } else if (sms_command.toLowerCase().equalsIgnoreCase("admin.getactiveusers")) {
                                               if (isAdmin) {
                                                    msg.append("Active users: ");
                                                   
                                                   Iterator<Map.Entry<Instant, String> >  iterator =  activeUsers.getActiveUsers().entrySet().iterator();
  
                                                    // Iterate over the HashMap
                                                    while (iterator.hasNext()) {
                                                        // Get the entry at this iteration
                                                        Map.Entry<Instant, String>  entry = iterator.next();
                                                        msg.append(entry.getValue()).append(", ");
                                                    }
                                                  
                                                   msg.delete(msg.lastIndexOf(","), msg.length());
                                                   if (activeUsers.getActiveUsers().isEmpty()){
                                                       msg.append(" no active users.");
                                                   }
                                               } else {
                                                   msg.append("You are not a admin to execute a command ");
                                                   msg.append(sms_command);
                                               }
                                           } else if (sms_command.toLowerCase().contains("admin.login.user.")) {
                                               if (isAdmin) {
                                                   int  indx = "admin.login.user.".length();
                                                   String user_to_login = sms_command.substring(indx);
                                                   if (isValidUserAPI(user_to_login)){
                                                       if (activeUsers.isActiveUser(user_to_login)){
                                                           msg.append("User ").append(user_to_login).append(" is already log in\n");
                                                       } else {
                                                           if (activeUsers.addUser(user_to_login)){
                                                               admin_login=true;
                                                               admin_login_phone_number=getPhoneForUserAPI(user_to_login);
                                                               msg.append("User ").append(user_to_login).append(" was log in successfully by ").append(getUserAPIForPhone(from)).append("\n");
                                                               archiveToFile.addToRepository("audit.out", "Login/SA/\t" + from + "\t" + user_to_login); 
                                                           } else {
                                                               msg.append("Log in failed ").append(user_to_login).append("\n");
                                                           }
                                                       }
                                                       
                                                   } else {
                                                       msg.append("A login name ").append(user_to_login).append(" wasn't found\n"); 
                                                   }
                                               } else {
                                                   msg.append("You are not a admin to execute a command ");
                                                   msg.append(sms_command);
                                               }
                                           } else if (sms_command.toLowerCase().contains("admin.logout.user.")) {
                                               if (isAdmin) {
                                                   int  indx = "admin.logout.user.".length();
                                                   String user_to_logout = sms_command.substring(indx);
                                                   if (isValidUserAPI(user_to_logout)){
                                                       if (!activeUsers.isActiveUser(user_to_logout)){
                                                           msg.append("User ").append(user).append(" is not login");
                                                       } else {
                                                           if (activeUsers.deleteUser(user_to_logout)){
                                                               admin_login=true;
                                                               admin_login_phone_number=getPhoneForUserAPI(user_to_logout);
                                                               msg.append("User ").append(user_to_logout).append(" was log out successfully by ").append(getUserAPIForPhone(from)).append("\n");
                                                               archiveToFile.addToRepository("audit.out", "Logout/SA/" + from + "\t" + user_to_logout);
                                                           } else {
                                                               msg.append("Log out failed ").append(user_to_logout);
                                                           }
                                                       } 
                                                   } else {
                                                       msg.append("A login name ").append(user_to_logout).append(" wasn't found\n");
                                                   }
                                               } else {
                                                   msg.append("You are not a admin to execute a command ");
                                                   msg.append(sms_command);
                                               }
                                           } else if (sms_command.toLowerCase().contains("admin.restart.server")) {
                                               if (isAdmin) {
                                                   Messaging.addSMStoQueue(from, "RESTART SERVER PROCESSING...", false);
                                                   try {
                                                       Thread.sleep(7000);
                                                   }
                                                   catch (InterruptedException e)    {
                                                       Thread.currentThread().interrupt(); // restore interrupted status
                                                   }
                                                   restart();
                                               } else {
                                                   msg.append("You are not a admin to execute a command ");
                                                   msg.append(sms_command);
                                               }
                                           } else if (sms_command.toLowerCase().contains("admin.execute.")) {
                                               if (isAdmin) {
                                                   int  indx = "admin.execute.".length();
                                                   String command_to_exec = sms_command.substring(indx);
                                                   if (command_to_exec.contains("rm")) {
                                                       msg.append("Command from black list ").append(sms_command).append(" (Not executed). \n");
                                                   } else {
                                                       _ScriptClass.runScript(command_to_exec);
                                                       msg.append("Response from command ");
                                                       msg.append(command_to_exec);
                                                       msg.append(" : ");
                                                       msg.append(_ScriptClass.getCommandReturn());
                                                   }
                                               } else {
                                                   msg.append("You are not a admin to execute a command ");
                                                   msg.append(sms_command);
                                               }
                                           } else if (sms_command.toLowerCase().contains("admin.at.command.")) {
                                               if (isAdmin) {
                                                   int  indx = "admin.at.command.".length();
                                                   String command_to_exec = sms_command.substring(indx);
                                                   UUID uuid = Messaging.addtoMessaging(command_to_exec+"\n", false, true);
                                                   msg.append("Add command ").append(command_to_exec).append(" to queue ");
                                                   msg.append("with uuid ").append(uuid );
                                               } else {
                                                   msg.append("You are not a admin to execute a command ");
                                                   msg.append(sms_command);
                                               }
                                           } else if (sms_command.toLowerCase().contains("admin.at.restart")) {
                                               if (isAdmin) {
                                                   Messaging.addtoMessaging("AT+CFUN=0\n", false, true);
                                                   Messaging.addtoMessaging("AT+CFUN=1\n", false, true);
                                                   
                                                   msg.append("AT RESTARTED\n");
                                               } else {
                                                   msg.append("You are not a admin to execute a command ");
                                                   msg.append(sms_command);
                                               }
                                           } else if (sms_command.toLowerCase().contains("admin.kill.self")) {
                                               if (isAdmin) {
                                                   Messaging.addSMStoQueue(from, "KILL.SELF SYSTEM PROCESSING...", false);
                                                   disasterDump("kill");
                                                   try {
                                                       Thread.sleep(10000);
                                                   }
                                                   catch (InterruptedException e)    {
                                                       Thread.currentThread().interrupt(); // restore interrupted status
                                                   }
                                                   System.exit(-100);
                                               } else {
                                                   msg.append("You are not a admin to execute a command ");
                                                   msg.append(sms_command);
                                               }
                                           } else if (sms_command.toLowerCase().startsWith("login.")) {
                                               if (activeUsers.isActiveUser(user)){
                                                   msg.append("User ").append(user).append(" is already login\n");
                                               } else {
                                                   long timeout_h = 1;
                                                   try {                                        
                                                      timeout_h=Long.parseLong(sms_command.toLowerCase().replace("login.", "").replaceAll(" ", ""));
                                                   } catch (Exception h){                                                       
                                                   }                                                           
                                                   
                                                  if (activeUsers.addUser(user, timeout_h*60)){
                                                      msg.append("User ").append(user).append(" was log in successfully. Automatic logout set to ").append(timeout_h).append(" hour(s)\n");
                                                      msg.append("Auto logout time: ").append(activeUsers.getLogoutTimeUsers(user)).append(".\n");
                                                      archiveToFile.addToRepository("audit.out", "Login/S\t" + from);                                                       
                                                  } else {
                                                       msg.append("Log in failed ").append(user).append("\n");
                                                 }
                                               }
                                           } else if ("login".equalsIgnoreCase(sms_command)) {
                                               if (activeUsers.isActiveUser(user)){
                                                   msg.append("User ").append(user).append(" is already login\n");
                                               } else {
                                                   if (activeUsers.addUser(user)){
                                                       msg.append("User ").append(user).append(" was log in successfully.\n");
                                                       msg.append("Auto logout time: ").append(activeUsers.getLogoutTimeUsers(user)).append(".\n");
                                                       
                                                       archiveToFile.addToRepository("audit.out", "Login/S\t" + from);
                                                       
                                                   } else {
                                                       msg.append("Log in failed ").append(user).append("\n");
                                                   }
                                               }
                                           } else if ("logout".equalsIgnoreCase(sms_command)) {
                                               if (!activeUsers.isActiveUser(user)){
                                                   msg.append("User ").append(user).append(" is not log in");
                                               } else {
                                                   if (activeUsers.deleteUser(user)){
                                                       msg.append("User ").append(user).append(" was log out successfully\n");
                                                       archiveToFile.addToRepository("audit.out", "Logout/S\t" + from); 
                                                   } else {
                                                       msg.append("Log out failed ").append(user);
                                                   }
                                               }
                                           } else {
                                               logger.debug("COMMAND " + sms_command);
                                               boolean next=false; 
                                               boolean isUserLogin = activeUsers.isActiveUser(user);
                                               if (!existsCommandInMap(sms_command)) {
                                                   msg.append("The command ").append(sms_command).append(" not found. ");
                                               } else {
                                                   List<Integer> intCommands =  getIntCommandForUserAPI(user);
                                                   boolean foundIngroup=false;
                                                   for (int u = 0; u<intCommands.size(); u++) {
                                                       if (commands_map.get(u).getGsmAias().equalsIgnoreCase(sms_command)) {
                                                           foundIngroup=true;
                                                           break;
                                                       }
                                                   }
                                                   if (!foundIngroup) {
                                                       msg.append("The command ");
                                                       msg.append(sms_command);
                                                       msg.append(" is not addressed for your user group(s) ");
                                                       List<String> gfu = getGroupsForUserAPI(user);
                                                       gfu.stream().map((_u) -> {
                                                           msg.append(_u);
                                                           return _u;
                                                       }).forEachOrdered((_item) -> {
                                                           msg.append(" ");
                                                       });
                                                       msg.append(". ");
                                                   } else {
                                                       for (int u = 0; u<intCommands.size(); u++) {
                                                           int id = intCommands.get(u);
                                                           if (id > commands_map.size()){
                                                               logger.error("id > commands_map.size()");
                                                               continue;
                                                           }
                                                           if (commands_map.get(id).getGsmAias().equalsIgnoreCase(sms_command)) {
                                                               boolean logiRequired = commands_map.get(id).getLoginRequired();
                                                               next=false;
                                                               if (logiRequired) {
                                                                   if (isUserLogin) {
                                                                       next=true;
                                                                   } else {
                                                                       msg.append("User ").append(user).append(" is not log in. ");
                                                                       msg.append("The command ");
                                                                       msg.append(sms_command);
                                                                       msg.append(" log in required!!!");
                                                                       next=false;
                                                                   }
                                                               } else {
                                                                   //nie potrzebuje logowania
                                                                   next=true;
                                                               }
                                                               if (next) {
                                                                   
                                                                   _ScriptClass.runScript(commands_map.get(id).getCommandForUser(user));
                                                                   String res="Response from command.";
                                                                   String res_error="Response from command.";
                                                                   
                                                                   if (commands_map.get(id).getResponse().length()==0){
                                                                       commands_map.get(id).setResponse(res);
                                                                   }
                                                                   
                                                                   if (commands_map.get(id).getResponse_error().length()==0){
                                                                       commands_map.get(id).setResponse_error(res_error);
                                                                   }
                                                                   
                                                                   if (commands_map.get(id).getResponseForUser(user).length()==0){
                                                                       commands_map.get(id).setResponseForUser(user, commands_map.get(id).getResponse());
                                                                   }
                                                                   
                                                                   if (commands_map.get(id).getResponseErrorForUser(user).length()==0){
                                                                       commands_map.get(id).setResponseErrorForUser(user, commands_map.get(id).getResponse_error());
                                                                   }
                                                                   
                                                                   if (_ScriptClass.getResponse_code()==0){
                                                                       msg.append(commands_map.get(id).getResponseForUser(user));
                                                                       msg.append(" ");
                                                                       msg.append(_ScriptClass.getCommandReturn()); 
                                                                   } else {
                                                                       msg.append(commands_map.get(id).getResponseErrorForUser(user));
                                                                       msg.append(" ");
                                                                       msg.append(_ScriptClass.getCommandReturn()); 
                                                                   }
                                                                    
                                                                   
                                                                  
                                                                  
                                                                  
                                                               } //next
                                                           } //end if
                                                       } //end int commands map
                                                   } //end else group
                                                   intCommands.clear();
                                               } //exist command
                                           }
                                       }

                                       logger.debug("MSG to send " + msg.toString());
                                       Messaging.addSMStoQueue(from, msg.toString(), false);
                                       
                                       if  (admin_login){
                                            logger.debug("MSG to send remote login/logout " + msg.toString());
                                            Messaging.addSMStoQueue(admin_login_phone_number, msg.toString(), false);
                                       }
                                                               
                                                                        
                                       try {
                                            Thread.sleep(1000);
                                        }
                                        catch (InterruptedException e)    {
                                            Thread.currentThread().interrupt(); // restore interrupted status
                                        }

                                  } else { //nieznany nadawca
                                
                                       String sms_commands  = sms_es[j+1].replaceAll("\\s+", "").replaceAll("OK", "").replaceAll("ERROR", "");
                                   
                                       logger.debug("sms: " + sms);

                                       if (sms_commands.equalsIgnoreCase("heartbeat")){
                                           msg.append("Hello, I'm alive !!!!"); 
                                       
                                           Messaging.addSMStoQueue(from, msg.toString(), false);

                                           try {
                                                Thread.sleep(1000);
                                            }
                                            catch (InterruptedException e)    {
                                                Thread.currentThread().interrupt(); // restore interrupted status
                                            }
                                       } else { //nieznany nadawca i nie pytanie o heartbeat
                                             msg.append("Unknown user send sms to jSmsServer. "); 
                                             msg.append("User: ");
                                             msg.append(from); 
                                             msg.append(". Text: ");
                                             msg.append(sms_es[j+1]); 
                                             usersApi.stream().filter((u) -> (u.isRedirectUnknownSms())).forEachOrdered((u) -> {
                                                 Messaging.addSMStoQueue(u.getPhone(), msg.toString(), false);
                                           });
                                           
                                       }

                                 }
                      

                             
                            }


                      }
                }
            }

            Messaging.deleteFromMessaging(hand_uuid, true, 0);
            logger.debug("****** SMS READ done  *****");
            
      }
       
       
     private static String calcMD5(String md5) {
         try   {
           MessageDigest md = MessageDigest.getInstance("MD5");
           byte[] array = md.digest(md5.getBytes());
           StringBuilder _sb = new StringBuilder();
           for (int i = 0; i < array.length; i++) {
                _sb.append(Integer.toHexString(array[i] & 0xFF | 0x100).substring(1, 3));
           }
           return _sb.toString();
         }
         catch (NoSuchAlgorithmException e) {}
         return null;
     }
     
    public List<String> getUniqGroupName(List<UserPhonebook> props){
        List<String> unique_groupName =  new ArrayList<>();

        
        props.forEach((UserPhonebook prop) -> {
            boolean uniq = true;
            
            for (String gr:prop.getGroups()){
                uniq = true;
                for (String q : unique_groupName){
                    if (gr.equalsIgnoreCase(q)){
                        uniq = false;
                    }
                }
                if (uniq) unique_groupName.add(gr);
            }
        });
        
        return unique_groupName;
        
    }
    
    public boolean isValidUserAPI(String username){
            boolean ret = false;
            for (UserApi user : usersApi){
                if (user.getUsername().equalsIgnoreCase(username)){
                    ret = true;
                    break;
                }
            }
            
            return ret;
        }     
        
        public boolean isAdminUserAPIForPhone(String phone){
            boolean ret = false;
            for (UserApi user : usersApi){
                if (user.getPhone().equalsIgnoreCase(phone)){
                    ret = user.isAdmin();
                    break;
                }
            }
            
            return ret;
        }  
        
        public String getPhoneForUserAPI(String _user){
            String ret="default";
            
            for (UserApi user : usersApi){
                if (user.getUsername().equalsIgnoreCase(_user)){
                    ret = user.getPhone();
                    break;
                }
            }
            
            return ret;
        }    
        
        
      public List<String> getGroupsForUserAPI(String _user){
            
            List<String> ret= new ArrayList<>();
            
            for (UserApi user : usersApi){
                if (user.getUsername().equalsIgnoreCase(_user)){
                    ret = user.getGroups();
                    break;
                }
            }
            
            return ret;
      } 
      
       public List<Integer> getIntCommandForUserAPI(String _user){
            List<String> groups_tmp  =  getGroupsForUserAPI(_user);
            List<Integer> ret= new ArrayList<>();
            
            for (int i=0; i<commands_map.size(); i++){
                   List<String> split_group_for_cmd = commands_map.get(i).getGroups();
                   boolean found_group = false;

                   for (String sgfc : split_group_for_cmd){
                       for (String sgfu : groups_tmp){
                           if (sgfu.equals(sgfc)){
                               found_group=true;
                               break;
                           }
                       }
                       if (found_group) break;
                   }
                   
                   if (found_group) ret.add(i);
            }
                        
            return ret;
      } 
      
       public boolean existsCommandInMap(String _cmd){
           
           boolean exist=false;
            
            for (int i=0; i<commands_map.size(); i++){
                if (commands_map.get(i).getGsmAias().equalsIgnoreCase(_cmd)){
                     exist=true;
                     break;
                }
                   
            }
            
            
            return exist;
      } 
      
  
      public List<String> getGroupsForUserPhonebook(String _user){
            
            List<String> ret= new ArrayList<>();
            
            for (UserPhonebook  user : usersPhonebook){
                if (user.getUsername().equalsIgnoreCase(_user)){
                    ret = user.getGroups();
                    break;
                }
            }
            
            return ret;
     }  
      
   
        
        public boolean isActiveUserAPIPhone(String phone){
            boolean ret = false;
            String _user = getUserAPIForPhone(phone);
            
            if (activeUsers.getActiveUsers().containsValue(_user)) {
                ret=true;
            }

            return ret;
        }
        
        
         public Set<Object> getAllKeys(Properties prop){
            Set<Object> keys = prop.keySet();
            return keys;
        }

        public String getPropertyValue(Properties prop, String key){
            return prop.getProperty(key);
        }      
        
        public String uptimeHumanReadable(Duration duration){
            StringBuilder upt = new StringBuilder ();
            upt.append("Days: ").append(duration.toDays()).append(", hours: ").append((duration.toHours()-duration.toDays()*24)).
                    append(", min: ").append((duration.toMinutes()-duration.toHours()*60)).append(", sec: ").append((duration.toMillis()/1000-duration.toMinutes()*60));
            return upt.toString();
            
        }
        
        
          public void disasterRecovery(){
            logger.info("*** disasterRecovery ***");
            Messaging.loadFromFiles();  
          
            logger.info("*** Recovery users ***");
               
            Properties logout = readUsersLogout();
            if (logout != null){
                 Set<Object> keys = logout.keySet();
                 for (Object key : keys){
                     String time = logout.getProperty(key.toString());
                     try {
                         logger.info(key.toString() + " | " + time);
                         activeUsers.addUser(key.toString(), Long.parseLong(time));
                     }  catch (Exception igonore){}                    
                 }
            }
            
           logger.info("*** disasterRecovery DONE ***");
        }
        
        public void disasterDump(String code){
           try {
            
               Messaging.lockWhenDumpLoad=true;

               archiveToFile.addToRepository("runs.out", "STOP_DISASTER ("+code+")\t" + sdf.format(new Date()));  

               usersApi.forEach((user) -> {
                   boolean tick=false;
                   /*for (String _user : activeUsers.getActiveUsers()){  //wszyscy aktywni uzytkownicy ktorzy musza sie logowac
                       if (_user.equals(user.getUsername())){
                           if (!user.isActiveAtStartup()){
                               String phone = user.getPhone();
                               Messaging.addSMStoQueue(phone, "The SMS server has detected a critical error (" + code +  ", tm=" + sdf_dis.format(new Date()) + ") and was automatically restarted. Please sign in again.", false);
                               tick=true;
                           }
                       }
                   }*/
                    if (!tick) {
                        if (user.isActiveAtStartup() && user.isAdmin()) {
                            String phone = user.getPhone();
                            Messaging.addSMStoQueue(phone, "The SMS server has detected a critical error (" + code +  ", tm=" + sdf_dis.format(new Date()) + ") and was automatically restarted. Quickly show your server logs.", false);
                        }
                    }
                });         

               ATCommandsManager.lockWrite();
               Messaging.dumpToFiles();
               
           } catch (Exception e){
               logger.error(e);
           }
            
        }    
        
        
       public static void stopDump(){
           Messaging.lockWhenDumpLoad=true;
           ATCommandsManager.lockWrite();
             
           archiveToFile.addToRepository("runs.out", "STOP\t" + sdf.format(new Date()));
           Properties dump = new Properties();
            
           usersApi.forEach((UserApi user) -> {
              // boolean tick=false; 
               if (user.isAdmin() && validTime()){
       
                   String phone = user.getPhone();
                   Messaging.addSMStoQueue(phone, "The SMS server was restarted..", false);
               }
             
               if (!user.isActiveAtStartup()) {
                    
                    /*for (int i=0; i<activeUsers.getActiveUsers().size(); i++){
                        if (activeUsers.getActiveUsers().get(i).equals(user.getUsername())){
                            if (!user.isActiveAtStartup()){
                                long tm = activeUsers.getLogoutTimeUsers().get(i).toEpochMilli() - System.currentTimeMillis();
                                tm = tm/60000;                                
                                dump.setProperty(activeUsers.getActiveUsers().get(i), ""+tm);
                                //Messaging.addSMStoQueue(user.getPhone(), "The SMS server was restarted. Please sign in again..", false);
                                
                            }  
                        }
                    }*/  
                    
                       Iterator<Map.Entry<Instant, String> >  iterator =  activeUsers.getActiveUsers().entrySet().iterator();
  
                        // Iterate over the HashMap
                        while (iterator.hasNext()) {

                            // Get the entry at this iteration
                            Map.Entry<Instant, String>  entry = iterator.next();

                            // Check if this value is the required value
                            if (user.getUsername().equals(entry.getValue())) {  
                                   if (!user.isActiveAtStartup()){
                                        long tm = entry.getKey().toEpochMilli() - System.currentTimeMillis();
                                         tm = tm/60000;                                
                                         dump.setProperty(entry.getValue(), Long.toString(tm));
                                   }
                                
                            }
                        } 
                   
                }
            });
           
            writeUsersLogout (dump); 
            
            Messaging.dumpToFiles();
            
        }
       
       public void restart(){
              
             logger.info("RESTART FUNCTION.....");
             
              Thread restartThread = new Thread(() -> {
                  try {
                      new ScriptExecutor().runScript(serverApi.getRestartScript());
                      logger.info("RESTARTED.....");
                  } catch (Exception ex) {
                      logger.error("RESTART " + ex);
                  }
             });
              restartThread.setName("RESTART");
              restartThread.setDaemon(true);
              restartThread.start();
               
       }
       
    public static void writeUsersLogout(Properties dump){
       String _file="messaging/logout";
        
       if (OSValidator.isUnix()){
              _file = absolutePath + "/" + _file;
        }   
           
        OutputStream output = null;
  	
        try {

		output = new FileOutputStream(_file);

		// save properties to project root folder
		dump.store(output, null);

	} catch (IOException io) {
		logger.error(io);
	} finally {
		if (output != null) {
			try {
				output.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}

	}
    }
    
    public static Properties readUsersLogout(){
       InputStream input = null;
       String _file="messaging/logout";
       Properties logout = new Properties();
        
       if (OSValidator.isUnix()){
              _file = absolutePath + "/" + _file;
        }   
       
        File stFile = new File(_file);
        try { 
            stFile.createNewFile();
        } catch (IOException ex) {
            logger.error("Nie utworzono pliku do statystyk.");
        } 	

	try {

		input = new FileInputStream(_file);

		// load a properties file
		logout.load(input);


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
        
        return logout;
    }
    
}
