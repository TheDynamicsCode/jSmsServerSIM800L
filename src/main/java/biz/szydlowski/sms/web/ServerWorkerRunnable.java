package biz.szydlowski.sms.web;

import biz.szydlowski.sms.api.CommandApi;
import biz.szydlowski.sms.Messaging;
import biz.szydlowski.sms.Version;
import biz.szydlowski.sms.WorkingStats;
import biz.szydlowski.sms.activeUsers;
import static biz.szydlowski.sms.Messaging.sms_sender;
import static biz.szydlowski.sms.WorkingObjects.SMS_ExpireDate;
import static biz.szydlowski.sms.WorkingObjects.TOKEN;
import static biz.szydlowski.sms.WorkingObjects.TOKEN_OLD;
import static biz.szydlowski.sms.WorkingObjects.apiKey;
import static biz.szydlowski.sms.WorkingObjects.archiveToFile;
import static biz.szydlowski.sms.WorkingObjects.atErrorThreshold;
import static biz.szydlowski.sms.WorkingObjects.smsCommands;
import static biz.szydlowski.sms.WorkingObjects.smsTepmlatesApi;
import static biz.szydlowski.sms.WorkingObjects.uniquePhonebook_groupName;
import static biz.szydlowski.sms.WorkingObjects.usersApi;
import static biz.szydlowski.sms.WorkingObjects.usersPhonebook;
import biz.szydlowski.sms.api.SmsTepmlateApi;
import biz.szydlowski.sms.api.UserApi;
import biz.szydlowski.sms.api.UserPhonebook;
import biz.szydlowski.sms.setting.CommandsMaps;
import biz.szydlowski.sms.utils.Statistics;
import com.alibaba.fastjson.JSON;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import static biz.szydlowski.sms.WorkingObjects.restart_watchdog;
import static biz.szydlowski.sms.WorkingObjects.kill_watchdog;
import static biz.szydlowski.sms.WorkingObjects.serverApi;
import static biz.szydlowski.sms.WorkingObjects.validatorApi;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerWorkerRunnable implements Runnable {
   
     static final Logger logger =  LogManager.getLogger(ServerWorkerRunnable.class);
      protected Socket clientSocket = null;
      protected SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      private boolean isAdmin=false;
      private boolean isApi=false;
        
      public ServerWorkerRunnable(Socket clientSocket, boolean api, boolean admin) {
            this.clientSocket = clientSocket;
            this.isAdmin=admin;
            this.isApi=api;
      }


      @Override
      public void run() {
        try {
          
          logger.info("Accepted Client : Address - " + clientSocket.getInetAddress().getHostName());
       
          InputStream input = this.clientSocket.getInputStream();
          String userInput = "default";

          BufferedReader stdIn = null;
          try {
                stdIn = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                userInput = stdIn.readLine();
          } catch (IOException ex) {
                logger.error(ex);
          }

          if (userInput == null) userInput = "DEFAULT";

          PrintWriter out = new PrintWriter(this.clientSocket.getOutputStream(), true);
        
          logger.debug(userInput);
          boolean isGet=true;
          boolean isPost=false;
          
          
          if (!(isAdmin || isApi)) {
           
             String data="<h1>401 UNAUTHORIZED. Authentication required.</h1><b><br/>Sorry, you are not allowed to access this page.</b><br/>";
             String response = "HTTP/1.1 401 UNAUTHORIZED\r\n" +
                    "Content-Length: "+data.length()+"\r\n" +
                    "Content-Type: text/html\r\n\r\n" +
                    data;
              
             out.print(response);
             out.flush();

             out.close();
             input.close();
             
             archiveToFile.addToRepository("reject.out", new StringBuilder().append(clientSocket.getInetAddress()).toString());

             return;
         } else {
                      
            archiveToFile.addToRepository("connect.out", new StringBuilder().append(clientSocket.getInetAddress()).toString());
         }
          
          isGet = userInput.contains("GET");
          
          String postData = "";
          if ( userInput.contains("POST")){
               isPost=true;
             
               String line;
               int postDataI=0;
                while ((line = stdIn.readLine()) != null && (line.length() != 0)) {
                    logger.debug("HTTP-HEADER: " + line);
                    if (line.contains("Content-Length:")) {
                        postDataI = Integer.parseInt(line.substring(line.indexOf("Content-Length:") + 16, line.length()));
                    }
                }
                postData = "";
                // read the post data
                if (postDataI > 0) {
                    char[] charArray = new char[postDataI];
                    stdIn.read(charArray, 0, postDataI);
                    postData = new String(charArray);
                }
                
                logger.debug("Raw post DATA " + postData);     
               
                postData =  replaceURL(postData );
                postData =  replacePolish(postData);
                                
                 
               logger.debug("post DATA after replace " + postData); 
             
          } else {
              isPost=false;
          }
         
           if ( isGet){
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html");
                    out.println("<html>\n");
                    out.println("<head>");
                    out.println("<title>");
                    out.println(Version.getVersion());
                    out.println("</title>");
                    out.println("</head>\n");
          }
           
           if (isPost){
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html");
                    out.println("<html>\n");
           }
          
          userInput = userInput.replace("GET", "");
          userInput = userInput.replace("HTTP/1.1", "");
          userInput = userInput.replace("/favicon.ico", "");
          userInput = userInput.replace("%20", " ");

          if (userInput.length() == 0){
             return;
          }
          UUID uuid = null;

         
          if ( userInput.contains("send_from_form.api") ){
              
               logger.debug("FORM API DATA " +  postData); 
               String[] spliter = postData.split("&");
            
               
               if ( spliter.length >= 2 && isPost) {
               
                   List<String> send_list =  new ArrayList<>();
                   boolean isProblem=false;
                   boolean setPhone=false;
                   boolean setMessage=false;
                   boolean setToken=false;
                   boolean isHidden=false;
                   
                   String user_or_group="default";
                   String txtmsg="default";
                   String token="default"; 
                   String hidden="false";
                  
                   for (int i=0; i<spliter.length; i++){
                        String [] _sp = spliter[i].split("=");
                        if (_sp.length==2){
                           if (_sp[0].equalsIgnoreCase("user") || _sp[0].equalsIgnoreCase("group") || _sp[0].equalsIgnoreCase("phone")){
                                String [] ss = _sp[1].split(";");
                                for (String sss:ss){
                                    send_list.add(sss.replaceAll("\\s+", ""));
                                } 
                                user_or_group=_sp[0];
                                setPhone=true;
                           } else  if (_sp[0].equalsIgnoreCase("message")){
                               txtmsg = _sp[1];
                                setMessage=true;
                           } else  if (_sp[0].equalsIgnoreCase("token")){
                               token = _sp[1];
                               setToken=true;
                           }  else  if (_sp[0].equalsIgnoreCase("hidden")){
                               hidden = _sp[1];
                           }  else { 
                               out.write(new StringBuilder().append("ERROR: SMS Server problem detected (E210)<br/>\n").toString());                               
                               isProblem=true;
                           }
                          
                        } else {
                            //out.write(new StringBuilder().append("ERROR: SMS Server problem detected (E215)<br/>\n").toString());
                            isProblem=true;
                        }
                   }
                
                 if (hidden.equals("true")) {
                     out.write(new StringBuilder().append("INFO: Hidden option enabled <br/>\n").toString());   
                     isHidden=true;
                 }
                         
                 if (!setPhone){
                     isProblem=true;
                     out.write(new StringBuilder().append("ERROR: phone number doesn't set<br/>\n").toString());
                 } 
                 
                 if (!setMessage){
                     isProblem=true;
                     out.write(new StringBuilder().append("ERROR: SMS Server problem detected (messages is empty?)<br/>\n").toString());
                 }    
                 
                 if (!setToken){
                     isProblem=true;
                     out.write(new StringBuilder().append("ERROR: SMS Server problem detected (token is empty?)<br/>\n").toString());
                 }    
                     
                   
                 if ( !(token.equalsIgnoreCase(TOKEN) || token.equalsIgnoreCase(TOKEN_OLD)) ){
                     isProblem=true;
                     out.write(new StringBuilder().append("ERROR: Invalid token ").append(token).toString());
                 }
                    
                 if (!isProblem){
                       switch (user_or_group) {
                           case "user":
                               txtmsg = replacePolish(txtmsg);
                               for (String send_user : send_list){
                                   
                                   String sendTo = getPhoneForUser(send_user);
                                   
                                   if (sendTo.equalsIgnoreCase("default")){
                                       out.write(new StringBuilder().append("ERROR: Unknown username ").append(send_user).append("\n").toString());
                                   } else {
                                       
                                       uuid = Messaging.addSMStoQueue(sendTo, txtmsg, isHidden) ;
                                       
                                       out.write(new StringBuilder().append("SMS send to ").append(send_user).append(" (").append(sendTo).append(")<br/>\n").toString());
                                       out.write(new StringBuilder().append("Add sms message ").append(txtmsg).append(" to queue ").toString());
                                       out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>\n").toString());
                                   }
                               }
                               break;
                           case "group":
                               List<String> unique_users =  new ArrayList<>();
                               boolean isUniq=true;
                               for (UserPhonebook u: usersPhonebook){ //kontakty
                                   
                                   String user = u.getUsername();
                                   
                                   for (String gr : u.getGroups()){
                                       for (String sgroup : send_list){
                                           if (gr.equalsIgnoreCase(sgroup)){
                                               isUniq=true;
                                               for (String us:unique_users){
                                                   if (us.equals(user)){
                                                       isUniq=false;
                                                       break;
                                                   }
                                               }
                                               if (isUniq) unique_users.add(user);
                                           }
                                       }
                                   }
                               }
                               txtmsg = replacePolish(txtmsg);
                               for (String send_user : unique_users){
                                   
                                   String sendTo = getPhoneForUser(send_user);
                                   
                                   if (sendTo.equalsIgnoreCase("default")){
                                       out.write(new StringBuilder().append("ERROR: Unknown username ").append(send_user).append("\n").toString());
                                   } else {
                                       
                                       uuid = Messaging.addSMStoQueue(sendTo, txtmsg, isHidden) ;
                                       
                                       out.write(new StringBuilder().append("SMS send to ").append(send_user).append(" (").append(sendTo).append(")<br/>\n").toString());
                                       out.write(new StringBuilder().append("Add sms message ").append(txtmsg).append(" to queue ").toString());
                                       out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>\n").toString());
                                   }
                               }
                               break;
                           case "phone":
                               txtmsg = replacePolish(txtmsg);
                               for (String send_user : send_list){
                                   
                                   if (send_user.startsWith(validatorApi.getFormPhoneStartsWith()) &&
                                           send_user.length()>=validatorApi.getFormPhoneMinLength() &&
                                           send_user.length()<=validatorApi.getFormPhoneMaxLength() ){
                                       uuid = Messaging.addSMStoQueue(send_user, txtmsg, isHidden) ;
                                       
                                       out.write(new StringBuilder().append("SMS send to ").append(send_user).append("<br/>\n").toString());
                                       out.write(new StringBuilder().append("Add sms message ").append(txtmsg).append(" to queue ").toString());
                                       out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>\n").toString());
                                   } else {
                                       out.write(new StringBuilder().append("ERROR phone number ").append(send_user).append("<br/>\n").toString());
                                   }
                                   
                                   
                               }
                               break;
                           default:
                               break;
                       }
                    }
                    
                    send_list.clear();                    
                    out.flush();

                }
              
          
          }  else if ( userInput.contains("/admin/actions.api") || userInput.contains("actions.api") ){
               logger.debug("ACTIONS API DATA " +  postData); 
               String[] spliter = postData.split("&");
               String action="default";
               String user="default";             
             
               if ( spliter.length >= 2 && isPost) {
                
                   for (String spliter1 : spliter) {
                       String[] _sp = spliter1.split("=");
                       if (_sp.length==2){
                           if (_sp[0].equalsIgnoreCase("action")){
                               action = _sp[1];
                           } else  if (_sp[0].equalsIgnoreCase("user")){
                               user = _sp[1];
                           }  else {
                               out.write(new StringBuilder().append("ERROR: SMS Server problem detected (E410)<br/>\n").toString()); 
                           }
                           
                       } else {
                       }
                   }
                   
               }
               if (isAdmin(out, isGet)){   
                   if (action.equalsIgnoreCase("sendToken")){

                          boolean found = false;

                         for (UserApi u : usersApi) { //tylko kontakty API
                            if (u.getUsername().equals(user)) {
                              found = true;
                            }
                          }

                          if (found) {
                              out.println(new StringBuilder().append("Token was send to ").append(user).toString());
                              String sendTo = getPhoneForUser(user);
                              StringBuilder msg = new StringBuilder();

                              msg.append("CURRENT TOKEN: ").append(TOKEN).append(" (valid 10 minutes)"); 
                              uuid = Messaging.addSMStoQueue(sendTo, msg.toString(), false) ;


                          } else  {
                              out.println(new StringBuilder().append("User ").append(user).append(" not found <br/>").toString());
                          }
                     } else if (action.equalsIgnoreCase("sendUpdate")){

                          boolean found = false;

                         for (UserApi u : usersApi) { //tylko kontakty API
                            if (u.getUsername().equals(user)) {
                              found = true;
                            }
                          }

                          if (found) {
                              out.println(new StringBuilder().append("Update info was send to ").append(user).toString()); 
                              
                              String sendTo = getPhoneForUser(user);
                              StringBuilder msg = new StringBuilder();
                              msg.append("SMS EE Server was updated to version ").append(Version.getVersion());
                             
                              uuid = Messaging.addSMStoQueue(sendTo, msg.toString(), false) ;
                          

                          } else  {
                              out.println(new StringBuilder().append("User ").append(user).append(" not found <br/>").toString());
                          }
                     } else if (action.equalsIgnoreCase("sendWelcome")){

                          boolean found = false;

                         for (UserApi u : usersApi) { //tylko kontakty API
                            if (u.getUsername().equals(user)) {
                              found = true;
                            }
                          }

                          if (found) {
                              out.println(new StringBuilder().append("Welcome msg was send to ").append(user).toString()); 
                              
                              String sendTo = getPhoneForUser(user);
                              StringBuilder msg = new StringBuilder();
                              
                              msg.append("Welcome to SMS Server EE!!! Current version ").append(Version.getVersion()).append(". Your user name is ").append(user).append(". To get a supported commands please send help.\n");
                              uuid = Messaging.addSMStoQueue(sendTo, msg.toString(), false) ;
                          

                          } else  {
                              out.println(new StringBuilder().append("User ").append(user).append(" not found <br/>").toString());
                          }
                     } else if (action.equalsIgnoreCase("login")){

                              long timeout_login = 60L;
                              boolean found = false;

                              for (UserApi u : usersApi) { //api
                                if (u.getUsername().equals(user)) {
                                  timeout_login = u.getTimeoutInMinutes();
                                  found = true;
                                }
                              }

                           if (found) {
                            if (activeUsers.isActiveUser(user)) {
                              out.println(new StringBuilder().append("User ").append(user).append(" is already login<br/>").toString());
                            }
                            else if (activeUsers.addUser(user, timeout_login)) {
                              out.println(new StringBuilder().append("User ").append(user).append(" was log in successfully <br/>").toString());
                              String sendTo = getPhoneForUser(user);
                              StringBuilder msg = new StringBuilder();
                              msg.append("User ").append(user).append(" was log in successfully via the website.\n");
                              msg.append("Auto logout time: ").append(activeUsers.getLogoutTimeUsers(user)).append(".\n");
                                              

                              uuid = Messaging.addSMStoQueue(sendTo, msg.toString(), false) ;

                              archiveToFile.addToRepository("audit.out", new StringBuilder().append("Login/W\t").append(sendTo).toString());

                            } else {
                              out.println(new StringBuilder().append("Login failed ").append(user).append("<br/>").toString());
                            }
                          } else  {
                            out.println(new StringBuilder().append("Login ").append(user).append(" not found <br/>").toString());
                          }
                    } else if (action.equalsIgnoreCase("logout")){

                         if (!activeUsers.isActiveUser(user)) {
                                out.println(new StringBuilder().append("User ").append(user).append(" is not login<br/>").toString());
                         } else if (activeUsers.deleteUser(user)) {
                                out.println(new StringBuilder().append("User ").append(user).append(" was log out successfully<br/>").toString());
                                String sendTo = getPhoneForUser(user);
                                StringBuilder msg = new StringBuilder();                       

                                msg.append("User ").append(user).append(" was log out successfully via the website.\n");

                                uuid = Messaging.addSMStoQueue(sendTo, msg.toString(), false) ;

                                archiveToFile.addToRepository("audit.out", new StringBuilder().append("Logout/W\t").append(sendTo).toString());
                          } else {
                            out.println(new StringBuilder().append("Logout failed ").append(user).append("<br/>").toString());
                          }
                    }  else if (action.equalsIgnoreCase("restart")){
                            if (isAdmin(out, isGet)) {
                               restart_watchdog=true;
                               out.println("RESTARTING (add task to watchdog)....");
                           }
                    }   else if (action.equalsIgnoreCase("kill.self")){
                             if (isAdmin(out, isGet)) {  
                                kill_watchdog=true;
                                out.println("Server was killed (add task to watchdog)....");
                             }
                    }
                    out.flush();
               }
  
          }  else  if (userInput.contains("alert") || userInput.contains("base64Alert") ) {
               
                boolean isbase64Alert=false;
                if ( userInput.contains("base64Alert")){
                    isbase64Alert=true;
                    logger.debug("sms base64Alert");

                } else {
                    logger.debug("sms alert");
                }
                
                if (!isbase64Alert){
                     userInput =  replaceURL(userInput); 
                     userInput =  replacePolish(userInput);
                 }
               
               
                String[] spliter = userInput.split("/");
               
                if ( (spliter.length == 3 && isPost) || spliter.length >= 4) {
                    String user = spliter[2].replaceAll("\\s+", "");

                    StringBuilder txtMSG = new StringBuilder();
                    if (!isPost){
                         for (int i = 3; i < spliter.length; i++) {
                           txtMSG.append(spliter[i]);
                           if (i >= spliter.length - 1) continue; 
                           txtMSG.append("/");
                         }
                    } else {
                        txtMSG.append(postData);
                    }

                   logger.debug(new StringBuilder().append("User ").append(user).toString());
                   
                   if (isbase64Alert){
                       logger.debug(new StringBuilder().append("base64 txtMSG ").append(txtMSG.toString()).toString());
                   } else {
                       logger.debug(new StringBuilder().append("txtMSG ").append(txtMSG.toString()).toString());
                   }

                   
                   if (isbase64Alert){
                       String sdecoded = "default";
                       try {
                          byte[] decoded = Base64.getMimeDecoder().decode(txtMSG.toString());
                          sdecoded = new String(decoded, "UTF-8");
                       } catch (UnsupportedEncodingException e) {
                          sdecoded = "error in decoding command";
                          logger.error(e);
                       }
                       
                        sdecoded =  replacePolish(sdecoded);
                        
                        logger.debug(new StringBuilder().append("decocded txtMSG ").append(sdecoded).toString());
                        txtMSG.delete(0, txtMSG.length());
                        txtMSG.append(sdecoded);
                    }
                    if (txtMSG.length()==0){ 
                        
                            if (isGet){
                                out.write("WARNING<br/> txtMSG.length()==0.<br/> Message not send!");
                            } else {
                                out.write("WARNING\n txtMSG.length()==0.\n Message not send!");
                            }
                          logger.warn("txtMSG.length()==0");
                    } else if (user.equalsIgnoreCase("ALL_ACTIVE")) {
                         Iterator<Map.Entry<Instant, String> >  iterator =  activeUsers.getActiveUsers().entrySet().iterator();
                          while (iterator.hasNext()) {
                            Map.Entry<Instant, String>  entry = iterator.next();
                            String sendTo = getPhoneForUser(entry.getValue());
                            
                            uuid = Messaging.addSMStoQueue(sendTo, txtMSG.toString(), false) ;
          
                            if (isGet){
                                out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue<br/>").toString());
                                out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                            } else {
                                out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue\n").toString());
                                out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                            }
                        }

                    } else  if (user.contains("groups=")) {

                          user = user.replaceAll("groups=", "");
                          String [] split_group =  user.split(";");
                         
                         Iterator<Map.Entry<Instant, String> >  iterator =  activeUsers.getActiveUsers().entrySet().iterator();
                     
                          boolean found_all_groups=false;
                          
                          while (iterator.hasNext()) {
                               Map.Entry<Instant, String>  entry = iterator.next();
                              
                               List<String>  split_group_for_user = getGroupsForUserAPI(entry.getValue());
                               boolean found_group = false;

                               for (String sgfc : split_group ){
                                   for (String sgfu : split_group_for_user){
                                       if (sgfu.equals(sgfc)){
                                           found_group=true;
                                           break;
                                       }
                                   }
                                   if (found_group) break;
                               }

                               if (found_group) {
                                    String sendTo = getPhoneForUser(entry.getValue()); 
                                    found_all_groups=true;
                                    
                                    uuid = Messaging.addSMStoQueue(sendTo, txtMSG.toString(), false) ;

                                    if (isGet){
                                        out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue<br/>").toString());
                                        out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                                    } else {
                                        out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue\n").toString());
                                        out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                                    }
                               } else {
                                    
                               }
                              
                          }  
                          
                          if (!found_all_groups){
                               if (isGet){
                                    out.write(new StringBuilder().append("Not active users in this group<br/>").toString());
                               } else {
                                    out.write(new StringBuilder().append("Not active users in this group\n").toString());
                               }
                         }

                    } else  if (user.contains("phonebook_users=")) {

                          user = user.replaceAll("phonebook_users=", "");
                          String [] split_users =  user.split(";");
                          boolean found_all_users=false;
                          
                          for (UserPhonebook  _userPb : usersPhonebook) {
                               boolean found_user = false;

                               for (String sgfc : split_users ){
                                       if (_userPb.getUsername().equals(sgfc)){
                                           found_user=true;
                                           break;
                                       }
                                   
                                   if (found_user) break;
                               }

                               if (found_user) {
                                    String sendTo = _userPb.getPhone();
                                    found_all_users=true;
                                    
                                    uuid = Messaging.addSMStoQueue(sendTo, txtMSG.toString(), false) ;

                                    if (isGet){
                                        out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue<br/>").toString());
                                        out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                                    } else {
                                        out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue\n").toString());
                                        out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                                    }
                               } else {
                                  
                               }
                          }
                          
                            if (!found_all_users){
                                    if (isGet){
                                       out.write(new StringBuilder().append("User ").append(user).append(" not found<br/>").toString());
                                    } else {
                                       out.write(new StringBuilder().append("User ").append(user).append(" not found\n").toString());
                                    }
                           }

                    } else if (user.contains("phonebook_groups=")) {

                          user = user.replaceAll("phonebook_groups=", "");
                          String [] split_group =  user.split(";");
                          boolean found_all_groups=false;
                          
                          for (UserPhonebook  _userPb : usersPhonebook) {
                               List<String>  split_group_for_user = _userPb.getGroups(); 
                               boolean found_group = false;

                               for (String sgfc : split_group ){
                                   for (String sgfu : split_group_for_user){
                                       if (sgfu.equals(sgfc)){
                                           found_group=true;
                                           break;
                                       }
                                   }
                                   if (found_group) break;
                               }

                               if (found_group) {
                                    String sendTo = _userPb.getPhone();
                                    found_all_groups=true;
                                    
                                    uuid = Messaging.addSMStoQueue(sendTo, txtMSG.toString(), false) ;

                                    if (isGet){
                                        out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue<br/>").toString());
                                        out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                                    } else {
                                        out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue\n").toString());
                                        out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                                    }
                               } else {
                                    
                               }
                            
                          }
                          
                          if (!found_all_groups){
                                if (isGet){
                                         out.write(new StringBuilder().append("Not active users in this group<br/>").toString());
                                } else {
                                         out.write(new StringBuilder().append("Not active users in this group\n").toString());
                                }
                             }

                    } else  if (user.contains("permanently=")) {

                        user = user.replaceAll("permanently=", "");  
                        
                        if (user.length()>0){

                            String sendTo = getPhoneForUser(user); 

                            uuid = Messaging.addSMStoQueue(sendTo, txtMSG.toString(), false) ;

                            if (isGet){
                                out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue<br/>").toString());
                                out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                            } else {
                                out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue\n").toString());
                                out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                            }
                        } else {
                               if (isGet){
                                   out.write("User error x0015<br/>");
                               } else {
                                   out.write("User error x0015\n");
                               }
                        }


                   } else if (!activeUsers.isActiveUser(user)) {

                          logger.info(new StringBuilder().append("User ").append(user).append(" is not login. ").toString());
                          out.write(new StringBuilder().append("User ").append(user).append(" is not login. <br>").toString());

                    } else if (activeUsers.isActiveUser(user)) {

                          String sendTo = getPhoneForUser(user);

                          uuid = Messaging.addSMStoQueue(sendTo, txtMSG.toString(), false) ;

                          if (isGet){
                                out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue<br/>").toString());
                                out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                          } else {
                                out.write(new StringBuilder().append("Add sms message ").append(txtMSG).append(" to queue\n").toString());
                                out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                          }
                   } else {
                         if (isGet){
                             out.write(new StringBuilder().append("Unknown option<br/>").toString());
                         } else {
                             out.write(new StringBuilder().append("Unknown option\n").toString());
                         }
                   }
                  
                }  else { 
                    if (isGet){
                         out.write("User splitter error x0017<br/>");
                    } else {
                         out.write("User splitter error x0017\n");
                    }
                    logger.error("SPLITER");
                }

            }  else  if (userInput.contains("apiSecure")  ) {
                
                logger.debug("apiSecure " +  postData); 
                String username="usr";
                String key="key"; 
                String message="msg";
                boolean isbase64Alert = false; 
                
                try {
                    ApiJson apiJson = JSON.parseObject(postData, ApiJson.class);
                    username = apiJson.getUsername();
                    isbase64Alert = apiJson.isBase64();
                    message = apiJson.getMessage();
                    key = apiJson.getApiKey();
                } catch (Exception e){
                    logger.error(e);
                }
           
                
                if (key.equals(apiKey)){
                
                   // logger.debug("username " +  username); 
                   // logger.debug("message " +  message); 
                   // logger.debug("key " + key); 

                    if (  isPost ) {


                       logger.debug(new StringBuilder().append("User ").append(username).toString());

                       if (isbase64Alert){
                           logger.debug(new StringBuilder().append("base64 txtMSG ").append(message).toString());
                       } else {
                           logger.debug(new StringBuilder().append("txtMSG ").append(message).toString());
                       }


                       if (isbase64Alert){
                           String sdecoded = "default";
                           try {
                              byte[] decoded = Base64.getMimeDecoder().decode(message);
                              sdecoded = new String(decoded, "UTF-8");
                           } catch (UnsupportedEncodingException e) {
                              sdecoded = "error in decoding command";
                              logger.error(e);
                           }

                            sdecoded =  replacePolish(sdecoded);

                            logger.debug(new StringBuilder().append("decocded txtMSG ").append(sdecoded).toString());
                            message = sdecoded;
                        }

                        if (username.equalsIgnoreCase("ALL_ACTIVE")) {
                             Iterator<Map.Entry<Instant, String> >  iterator =  activeUsers.getActiveUsers().entrySet().iterator();
                        
                            while (iterator.hasNext()) {
                                Map.Entry<Instant, String>  entry = iterator.next();
                              
                                String sendTo = getPhoneForUser(entry.getValue());

                                uuid = Messaging.addSMStoQueue(sendTo, message, false) ;

                                if (isGet){
                                    out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue<br/>").toString());
                                    out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                                } else {
                                    out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue\n").toString());
                                    out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                                }
                            }

                        } else  if (username.contains("groups=")) {

                              username = username.replaceAll("groups=", "");
                              String [] split_group =  username.split(";");
                              Iterator<Map.Entry<Instant, String> >  iterator =  activeUsers.getActiveUsers().entrySet().iterator();
                          
                              boolean found_all_groups=false;

                              while (iterator.hasNext()) {
                                   Map.Entry<Instant, String>  entry = iterator.next();
                                   List<String>  split_group_for_user = getGroupsForUserAPI(entry.getValue());
                                   boolean found_group = false;

                                   for (String sgfc : split_group ){
                                       for (String sgfu : split_group_for_user){
                                           if (sgfu.equals(sgfc)){
                                               found_group=true;
                                               break;
                                           }
                                       }
                                       if (found_group) break;
                                   }

                                   if (found_group) {
                                        String sendTo = getPhoneForUser(entry.getValue()); 
                                        found_all_groups=true;

                                        uuid = Messaging.addSMStoQueue(sendTo, message, false) ;

                                        if (isGet){
                                            out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue<br/>").toString());
                                            out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                                        } else {
                                            out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue\n").toString());
                                            out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                                        }
                                   } else {

                                   }

                              }  

                              if (!found_all_groups){
                                   if (isGet){
                                        out.write(new StringBuilder().append("Not active users in this group<br/>").toString());
                                   } else {
                                        out.write(new StringBuilder().append("Not active users in this group\n").toString());
                                   }
                             }

                        } else  if (username.contains("phonebook_users=")) {

                              username = username.replaceAll("phonebook_users=", "");
                              String [] split_users =  username.split(";");
                              boolean found_all_users=false;

                              for (UserPhonebook  _userPb : usersPhonebook) {
                                   boolean found_user = false;

                                   for (String sgfc : split_users ){
                                           if (_userPb.getUsername().equals(sgfc)){
                                               found_user=true;
                                               break;
                                           }

                                       if (found_user) break;
                                   }

                                   if (found_user) {
                                        String sendTo = _userPb.getPhone();
                                        found_all_users=true;

                                        uuid = Messaging.addSMStoQueue(sendTo, message, false) ;

                                        if (isGet){
                                            out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue<br/>").toString());
                                            out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                                        } else {
                                            out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue\n").toString());
                                            out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                                        }
                                   } else {

                                   }
                              }

                                if (!found_all_users){
                                        if (isGet){
                                           out.write(new StringBuilder().append("User ").append(username).append(" not found<br/>").toString());
                                        } else {
                                           out.write(new StringBuilder().append("User ").append(username).append(" not found\n").toString());
                                        }
                               }

                        } else if (username.contains("phonebook_groups=")) {

                              username = username.replaceAll("phonebook_groups=", "");
                              String [] split_group =  username.split(";");
                              boolean found_all_groups=false;

                              for (UserPhonebook  _userPb : usersPhonebook) {
                                   List<String>  split_group_for_user = _userPb.getGroups(); 
                                   boolean found_group = false;

                                   for (String sgfc : split_group ){
                                       for (String sgfu : split_group_for_user){
                                           if (sgfu.equals(sgfc)){
                                               found_group=true;
                                               break;
                                           }
                                       }
                                       if (found_group) break;
                                   }

                                   if (found_group) {
                                        String sendTo = _userPb.getPhone();
                                        found_all_groups=true;

                                        uuid = Messaging.addSMStoQueue(sendTo, message, false) ;

                                        if (isGet){
                                            out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue<br/>").toString());
                                            out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                                        } else {
                                            out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue\n").toString());
                                            out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                                        }
                                   } else {

                                   }

                              }

                              if (!found_all_groups){
                                    if (isGet){
                                             out.write(new StringBuilder().append("Not active users in this group<br/>").toString());
                                    } else {
                                             out.write(new StringBuilder().append("Not active users in this group\n").toString());
                                    }
                                 }

                        } else  if (username.contains("permanently=")) {

                            username = username.replaceAll("permanently=", "");  

                            if (username.length()>0){

                                String sendTo = getPhoneForUser(username); 

                                uuid = Messaging.addSMStoQueue(sendTo, message, false) ;

                                if (isGet){
                                    out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue<br/>").toString());
                                    out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                                } else {
                                    out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue\n").toString());
                                    out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                                }
                            } else {
                                   if (isGet){
                                       out.write("User error x0015<br/>");
                                   } else {
                                       out.write("User error x0015\n");
                                   }
                            }


                       } else if (!activeUsers.isActiveUser(username)) {

                              logger.info(new StringBuilder().append("User ").append(username).append(" is not login. ").toString());
                              out.write(new StringBuilder().append("User ").append(username).append(" is not login. <br>").toString());

                        } else if (activeUsers.isActiveUser(username)) {

                              String sendTo = getPhoneForUser(username);

                              uuid = Messaging.addSMStoQueue(sendTo, message, false) ;

                              if (isGet){
                                    out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue<br/>").toString());
                                    out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                              } else {
                                    out.write(new StringBuilder().append("Add sms message ").append(message).append(" to queue\n").toString());
                                    out.write(new StringBuilder().append("with uuid ").append(uuid).append("\n").toString());
                              }
                       } else {
                             if (isGet){
                                 out.write(new StringBuilder().append("Unknown option<br/>").toString());
                             } else {
                                 out.write(new StringBuilder().append("Unknown option\n").toString());
                             }
                       }

                    }  else { 
                        if (isGet){
                             out.write("POST error x0018<br/>");
                        } else {
                             out.write("POST splitter error x0018\n");
                        }
                    }
                } else {
                     if (isGet){
                             out.write("Invalid ApiKey<br/>");
                     } else {
                             out.write("Invalid ApiKey\n");
                     }
             
                }

              } else if (userInput.contains("atcommand")) {
                    if (isAdmin(out, isGet)){
                     
                       String[] spliter = userInput.split("/");
                   

                       if (spliter.length == 3) {
                          String command = spliter[2];

                          uuid = Messaging.addtoMessaging(new StringBuilder().append(command).append("\n").toString(), false, true);

                          out.write(new StringBuilder().append("Add command ").append(command).append(" to queue<br/>").toString());
                          out.write(new StringBuilder().append("with uuid ").append(uuid).append("<br/>").toString());
                      }
                   }
              }  else if (userInput.contains("version")) {
                 printVersion(out);
                 
              }  else if (userInput.contains("jvm")) {
                 printBreak(out); 
                 printJVM(out);     
                 printBreak(out);  
                 printHomeAndBack(out);
              }  else if (userInput.contains("all_threads")) {
                 printBreak(out); 
                 printAllThreads(out);     
                 printBreak(out);  
                 printHomeAndBack(out);
              } else if (userInput.contains("uptime")) { 
                 printBreak(out); 
                 out.print("Uptime " + uptimeHumanReadable(WorkingStats.getUptime()) + "<br/>");  
                 printBreak(out);  
                 printHomeAndBack(out);
              } else if (userInput.contains("admin/stats")) {
                   if (isAdmin(out, isGet)){
                        Statistics _Statistics = new Statistics();
                        _Statistics.loadStats();
                        String stat = _Statistics.readStatsToHTMLString();
                        //stat=stat.replaceAll("\n", "<br/>");          
                        
                        out.println(stat);
                        printHomeAndBack(out);
                   }
              
              } else if (userInput.contains("admin/activeusers")){
                    if (isAdmin(out, isGet)){
                         StringBuilder sb =  new StringBuilder ();
                         sb.append("Active users: <br/>");
                         Iterator<Map.Entry<Instant, String> >  iterator =  activeUsers.getActiveUsers().entrySet().iterator();
                        
                         while (iterator.hasNext()) {
                              Map.Entry<Instant, String>  entry = iterator.next();
                              sb.append(entry.getValue()).append("<br/>");
                         }  
                         
                         if (activeUsers.getActiveUsers().isEmpty()){
                              sb.append("No active users.");
                         }   
                         out.println(sb.toString());
                         printHomeAndBack(out);
                    }
             }  else if (userInput.contains("admin/outboundsms")){
                    if (isAdmin(out, isGet)){
                         StringBuilder sb =  new StringBuilder ();
                         sb.append("Outbound text messages <br/>");
                         Messaging.getLastOutboundSms().stream().map((u) -> {
                             sb.append(u);
                            return u;
                        }).forEachOrdered((_item) -> {
                            sb.append("<br/>");
                        });  
                         printBreak(out);
                         out.println(sb.toString());
                         printBreak(out);
                         printHomeAndBack(out);
                    }
             }  else if (userInput.contains("admin/inboundsms")){
                    if (isAdmin(out, isGet)){
                         StringBuilder sb =  new StringBuilder (); 
                         sb.append("Inbound text messages <br/>");
                         Messaging.getLastInboundSms().stream().map((u) -> {
                             sb.append(u);
                            return u;
                        }).forEachOrdered((_item) -> {
                            sb.append("<br/>");
                        });  
                         printBreak(out);
                         out.println(sb.toString());
                         printBreak(out);
                         printHomeAndBack(out);
                    }
             }  else  if (userInput.contains("admin/smsexpiredate")){
                   if (isAdmin(out, isGet)) {
                        printBreak(out);   
                        out.println("SMS expire date: " + SMS_ExpireDate + "<br/>"); 
                        printBreak(out);  
                        printHomeAndBack(out);
                   }  
             }   else  if (userInput.contains("admin/smslasthour")){
                   if (isAdmin(out, isGet)) {
                        printBreak(out);   
                        out.println("SMS send (last hour): " +  sms_sender.size() + "<br/>");
                        for (long t : sms_sender){
                           long diff=(System.currentTimeMillis()-t)/1000;
                           out.println("SMS send " +  diff + " seconds ago..<br/>");
                        }
                        out.println("SMS send LIMIT (last hour): " +  serverApi.getMaxSmsInHour() + "<br/>"); 
                        printBreak(out);  
                        printHomeAndBack(out);
                   }  
             }  else if (userInput.contains("admin/dump_messaging")) {
                  if (isAdmin(out, isGet)) {
                        Messaging.dumpToFiles();
                        printBreak(out);
                        out.println(new StringBuilder().append("queue_uuid size: ").append(Messaging.getQueueSize()).append("<br/>").toString());
                        out.println(new StringBuilder().append("Dump was done").append("<br/>").toString());
                        printBreak(out);
                        printHomeAndBack(out);
                    }
              } else if (userInput.contains("admin/load_messaging")) {
                    if (isAdmin(out, isGet)){
                          Messaging.loadFromFiles();
                          printBreak(out);
                          out.println(new StringBuilder().append("queue_uuid size: ").append(Messaging.getQueueSize()).append("<br/>").toString());
                          out.println(new StringBuilder().append("Load was done").append("<br/>").toString());
                          printBreak(out);
                          printHomeAndBack(out);
                    }
                   
              } else if (userInput.contains("admin/messaging")) {
                    if (isAdmin(out, isGet)){
                        printBreak(out);
                        out.println(new StringBuilder().append("queue_uuid size: ").append(Messaging.getQueueSize()).append("<br/>").toString());

                        Queue queue_uuid_main = Messaging.getQueueUUID();

                        Iterator itr = queue_uuid_main.iterator();

                        while (itr.hasNext()) {
                          uuid = (UUID)itr.next();
                          String atcmd = Messaging.getAtMsgToSend(uuid);
                          out.println(new StringBuilder().append(uuid).append(" ").append(atcmd).append("<br/>").toString());
                        }
                        printBreak(out);
                        printHomeAndBack(out);
                    }
              } else if (userInput.contains("help")){
                   StringBuilder msg = new StringBuilder();   
                   smsCommands.forEach((sms_c) -> {
                       if (sms_c.startsWith("admin")){
                           msg.append("Only for admins: ");
                           msg.append(sms_c.replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
                           msg.append("<br/>");
                       } else {
                           msg.append(sms_c.replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
                           msg.append("<br/>");
                       }
                  });   
                  
                  printBreak(out);
                  out.write(msg.toString());
                  printBreak(out);
                  printHomeAndBack(out);
                                           
              } else if (userInput.contains("commandsMap")){
                   List<CommandApi> commands_map = new CommandsMaps().getCommandsApi();
                   
                  out.print("<h2>commands Map</h2>\n");   
                  out.println("<style>\n table, th, td {\n border: 1px solid black;\n border-collapse: collapse;\n }\n th, td {\n padding: 5px;\n");
                  out.println(" text-align: left;  \n}\n </style>");  
       
                  out.println("<table style=\"width:50%\">");    
                  out.println(" <tr>\n <th>Alias</th>\n <th>For groups</th>\n <th>Full command</th>\n </tr>");
        
                  commands_map.stream().map((cmd) -> {
                      //userzy api
                      out.println("<tr><td>");
                      out.print(cmd.getGsmAias());
                      return cmd;
                  }).map((cmd) -> {
                      out.println("</td>");
                      out.println("<td>");
                      out.print(cmd.printGroups());
                      return cmd;
                  }).map((cmd) -> {
                      out.println("</td>");
                      out.println("<td>");
                      out.print(cmd.getCommand());
                      return cmd;
                  }).map((_item) -> { 
                      out.println("</td>");
                      return _item;
                  }).forEachOrdered((_item) -> {
                      out.println("</tr>");
                  });
       
                  out.println("</table><br/>"); 
                  commands_map.clear();
                  printHomeAndBack(out);
                                           
              }else if (userInput.contains("pid")) {
                  printPID(out);
              } else if (userInput.contains("PID")) {
                  printPID(out);
              }  else if (userInput.contains("aterrorthreshold")) {
                   printAtErrorThreshold(out);
              }   else if (userInput.contains("sms_form_user")) {
                 printSendSmsUser(out);
              } else if (userInput.contains("sms_form_group")) {
                 printSendSmsGroup(out);
              }  else if (userInput.contains("sms_form_admin")) {
                 printSendSmsAdmin(out);
              } else if (userInput.contains("sms_form_template")) {
                 printSendSmsTemplate(out);
              }  else if (userInput.contains("admin/users")) {
                  if (isAdmin(out, isGet)) {
                        printUsers(out);
                    }
             }   else if (userInput.contains("admin/phonebook")) {
                    if (isAdmin(out, isGet)){
                        printPhonebook(out);
                    }
             }   else if (userInput.replaceAll("\\s+", "").equals("/")){                  
                 if (isGet){
                     printBreak(out);  
                     out.println(new StringBuilder().append("<b>SMS Server Enterprise Edition ").append(Version.getVersion()).append("</b>").append("<br/><a href=\"version\">Version</a><br/>").toString());
                     out.println(new StringBuilder().append("<a href=\"pid\">Display PID</a><br/>").toString());
                     out.println(new StringBuilder().append("<a href=\"aterrorthreshold\">Display AtErrorThreshold</a><br/>").toString());             
                     out.println(new StringBuilder().append("<a href=\"jvm\">Display JVM statistics</a><br/>").toString());
                     out.println(new StringBuilder().append("<a href=\"all_threads\">Display all threads</a><br/>").toString());
                     out.println(new StringBuilder().append("<a href=\"uptime\">Uptime</a><br/>").toString());     
                     out.println(new StringBuilder().append("<a href=\"help\">Help</a><br/>").toString());
                     out.println(new StringBuilder().append("<a href=\"commandsMap\">Display Commands Map</a><br/>").toString());
                     out.println(new StringBuilder().append("<a href=\"admin/stats\">Display statistics</a><br/>").toString());
                     out.println(new StringBuilder().append("<a href=\"admin/users\">Display Users</a><br/>").toString());  
                     out.println(new StringBuilder().append("<a href=\"admin/phonebook\">Display Phonebook</a><br/>").toString());   
                     out.println(new StringBuilder().append("<a href=\"admin/smsexpiredate\">SMS-EXPIRE-DATE</a><br/>").toString());
                     out.println(new StringBuilder().append("<a href=\"admin/smslasthour\">SMS-COUNT LAST HOUR</a><br/>").toString());
                     out.println(new StringBuilder().append("<a href=\"admin/messaging\">Display Messaging</a><br/>").toString()); 
                     
                     out.println(new StringBuilder().append("<a href=\"sms_form_user\">Send SMS to user</a><br/>").toString()); 
                     out.println(new StringBuilder().append("<a href=\"sms_form_group\">Send SMS to group</a><br/>").toString()); 
                     out.println(new StringBuilder().append("<a href=\"sms_form_admin\">Send SMS to phone number</a><br/>").toString()); 
                     out.println(new StringBuilder().append("<a href=\"sms_form_template\">Send SMS using template</a><br/>").toString()); 
                     
                     out.println(new StringBuilder().append("<a href=\"admin/outboundsms\">Show outbound SMS (last ").append(serverApi.getMaxOutboundMsgWeb()).append(")</a><br/>").toString());
                     out.println(new StringBuilder().append("<a href=\"admin/inboundsms\">Show inbound SMS (last ").append(serverApi.getMaxInboundMsgWeb()).append(")</a><br/>").toString());
                                                
                     out.println(new StringBuilder().append("<a href=\"admin/load_messaging\">Load Messaging</a> (WARN)<br/>").toString()); 
                     out.println(new StringBuilder().append("<a href=\"admin/dump_messaging\">Dump Messaging</a> (WARN)<br/>").toString()); 
                     
                    out.println("<br/><button onclick=\"Action('restart', 'admin')\">Restart SMS Server</button><br/>");
                    out.println("<button onclick=\"Action('kill.self', 'admin')\">Kill SMS Server</button><br/>");
                    printBreak(out);
              
                   
                    printActionsApiFunction(out);
                    printHomeAndBack(out);
                 }
              } else {
                 if (isGet) out.println(new StringBuilder().append("Page/Command not found").append("<br/>").toString());
                 else out.println(new StringBuilder().append("The command not found").append("<br/>").toString());

             }


          if  ( isGet  ) {
                 out.println("\n</html>\n");
          }
          
          out.flush();

          out.close();
          input.close();

          logger.debug("Request processed/completed...");
        }
        catch (IOException e) {
          logger.error(e);
        }
      }
      
      private void printBreak(PrintWriter out){
            out.println("========================================================<br/>");  
      }
     
      private void printActionsApiFunction(PrintWriter out){
          out.println("<font color=\"red\"><p id=\"info\"></p></font>");
      
          out.println(new StringBuilder().append("<script>\n").append("function Action(type, user) {\n")
                              .append( "    var txt = \'Action\';\n")
                              .append( "    if (type === 'sendToken') {\n")    
                              .append( "        txt = \"Are you sure you want to send a current TOKEN to user \" + user + \"?\"; \n")
                              .append( "    } else if (type === 'sendWelcome') {\n")
                              .append( "        txt = \"Are you sure you want to send a welcome message to user \" + user + \"?\";\n")
                              .append( "    } else if (type === 'sendUpdate') {\n")
                              .append( "        txt = \"Are you sure you want to send a update message to user \" + user + \"?\";\n")
                              .append( "    } else if (type === 'login') {\n")
                              .append( "        txt = \"Are you sure you want to login user \" + user + \"?\";\n")
                              .append( "    } else if (type === 'logout') {\n")
                              .append( "        txt = \"Are you sure you want to logout user \" + user + \"?\";\n")
                              .append( "    } else if (type === 'restart') {\n")
                              .append( "        txt = \"Are you sure you want to restart sms server?\";\n")
                              .append( "    } else if (type === 'kill.self') {\n")
                              .append( "        txt = \"Are you sure you want to kill sms server?\";\n")
                              .append( "    } \n")
                              .append( "    var r = confirm(txt);\n")
                              .append( "    if (r == true) {\n")
                              .append( "        var xhttp = new XMLHttpRequest();\n")
                              .append( "        xhttp.onreadystatechange = function() {\n")
                              .append( "           if (this.status == 200) {")
                              .append( "               document.getElementById(\"info\").innerHTML = this.responseText;\n")
                              .append( "           }\n")
                              .append( "       };\n")
                              .append( "       xhttp.open(\"POST\", \"actions.api\", true);")
                              .append( "       xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n")
                              .append( "       xhttp.send(\"action=\"+type+\"&user=\"+user);\n ")
                              .append( "    } else {\n")
                              .append( "    }\n")
                              .append(  " }\n")
                              .append( " </script>").toString());
            
      }
     
      private void printHomeAndBack(PrintWriter out){
          
          out.println("<br/><button onclick=\"goHome()\">HOME</button>\n");
          out.println("  "); 
          out.println("<script>\n");
          out.println("function goHome() {\n");
          out.println("   window.location = '/';\n");
          out.println("}\n" );
          out.println( "</script>");
          
          out.println("<button onclick=\"goBack()\">Go Back</button>\n");
          out.println("\n");
          out.println("<script>\n");
          out.println("function goBack() {\n");
          out.println("    window.history.back();\n");
          out.println("}\n" );
          out.println( "</script>");
     }

      private void printPID(PrintWriter out) {
        String processName = ManagementFactory.getRuntimeMXBean().getName();

        printBreak(out);
        out.println("<b> CURRENT PID ");
        out.println(Long.parseLong(processName.split("@")[0]));
        out.println(" </b><br/>");
        printBreak(out);
        printHomeAndBack(out);
      }  
      
     private void printAtErrorThreshold(PrintWriter out) {
        printBreak(out);
        out.println("<b> Current atErrorThreshold: ");
        out.println(atErrorThreshold);
        out.println(" </b><br/>");
        printBreak(out);
        printHomeAndBack(out);
      }
      
      

      private void printVersion(PrintWriter out)  {
        printBreak(out);
        out.println("<b> ");
        out.println(Version.getName());
        out.println(" </b><br/>");
        out.println(Version.getVersion());
        out.println("<br/>");
        out.println(Version.getAuthor());
        out.println("<br/>");
        out.println("DEV_VERSION_EXPIRE_STR: ");
        out.println(Version.DEV_VERSION_EXPIRE_STR);
        out.println("<br/>");
        printBreak(out);
        printHomeAndBack(out);
      }
    
     private void printPhonebook(PrintWriter out)  {
        out.print("<h2>Phonebook</h2>\n");   
        out.println("<style>\n table, th, td {\n border: 1px solid black;\n border-collapse: collapse;\n }\n th, td {\n padding: 5px;\n");
        out.println(" text-align: left;  \n}\n </style>");  
       
        out.println("<table style=\"width:50%\">");    
        out.println(" <tr>\n <th>User</th>\n <th>Groups</th>\n <th>Phone</th>\n </tr>");
        
        usersPhonebook.stream().map((user) -> {
            //userzy api
            out.println("<tr><td>");
            out.print(user.getUsername());
            return user;
        }).map((user) -> {
            out.println("</td>");
            out.println("<td>");
            out.print(user.printGroups());
            return user;
        }).map((user) -> {
            out.println("</td>");
            out.println("<td>");
            out.print(user.getPhone());
            return user;
        }).map((_item) -> {
            out.println("</td>");
            return _item;
        }).forEachOrdered((_item) -> {
            out.println("</tr>"); 
        });
       
        out.println("</table><br/>");
        
        out.println("<table style=\"width:50%\">");    
        out.println(" <tr>\n <th>Group</th>\n <th>Users</th>\n </tr>");
       
     
        for (String group : uniquePhonebook_groupName){ //userzy api
           out.println("<tr><td>");
           out.print(group);
           out.println("</td>");
           out.println("<td>"); 
           for (UserPhonebook user : usersPhonebook){ //userzy api
               
                boolean found=false;
                for (String sgr:user.getGroups()){
                    if (sgr.equalsIgnoreCase(group )){
                        found=true;
                        break;
                    }
                }
                
                if (found){  
                    out.print(user.getUsername());
                    out.print("  ");
                }
           }  
           out.println("</td>"); 
           out.println("</tr>"); 
        } 
       
        out.println("</table>");
  
        printHomeAndBack(out);
        
     }  
      
     private void printUsers(PrintWriter out)  {
        out.print("<h2>USERS</h2>\n");  
        out.println("<style>\n table, th, td {\n border: 1px solid black;\n border-collapse: collapse;\n }\n th, td {\n padding: 5px;\n");
        out.println(" text-align: left;   \n}\n </style>");  
       
        out.println("<table style=\"width:50%\">");    
        out.println(" <tr>\n <th>User</th>\n <th>Groups</th>\n  <th>Phone</th>\n  <th>activeAtStartup</th>\n   <th>redirectUnknownSms</th>\n  <th>Status</th>\n <th>Automatic logout time</th>\n <th>Login</th>\n <th>Logout</th>\n <th>Send welcome msg</th>\n <th>Send update msg</th> <th>Send current TOKEN</th>\n</tr>");
        
        for (UserApi user : usersApi){ //userzy api
            out.println("<tr><td>");
            out.print(user.getUsername());
            out.println("</td>");
            
    
            out.println("<td>");
            out.print(user.printGroups());
            out.println("</td>");    
            
            out.println("<td>");
            out.print(user.getPhone());
            out.println("</td>");
            
            
            out.println("<td>");
            out.print(user.isActiveAtStartup());
            out.println("</td>"); 
            
            out.println("<td>");
            out.print(user.isRedirectUnknownSms());
            out.println("</td>");
            
                     
                out.println("<td>");
            if (activeUsers.isActiveUser(user.getUsername())){
                out.println("ONLINE");  
            } else {
                out.println("LOGGED OUT");  
            }    
            
                     
            out.println("<td>");
            if (activeUsers.isActiveUser(user.getUsername())){
                out.println(activeUsers.getLogoutTimeUsers(user.getUsername()));  
            } else {
                out.println("----");  
            }            
            out.println("</td>");  
            
         
            out.println("</td>"); 
           
            out.println("<td><button onclick=\"Action('login', '"+user.getUsername()+"')\">LogIn</button><br/></td>");
            out.println("<td><button onclick=\"Action('logout', '"+user.getUsername()+"')\">LogOut</button><br/></td>");
            out.println("<td><button onclick=\"Action('sendWelcome', '"+user.getUsername()+"')\">Welcome</button><br/></td>");
            out.println("<td><button onclick=\"Action('sendUpdate','"+user.getUsername()+"')\">Update</button><br/></td>");
            out.println("<td><button onclick=\"Action('sendToken','"+user.getUsername()+"')\">TOKEN</button><br/></td>");
            out.println("</tr>");  
        } 
       
        out.println("</table>");
        
        printActionsApiFunction(out);
        
        printHomeAndBack(out);
        
      }
    
      
     
     private void printSendSmsGroup(PrintWriter out){   
         
         
           
            StringBuilder user_helper = new StringBuilder();
          
            uniquePhonebook_groupName.forEach((group) -> {
                user_helper.append("<option value=\"").append(group).append("\">").append(group).append("</option>\n");
            });
         
            out.println("<h2>Send SMS to GROUP</h2>\n");
            out.println("<form method=\"post\" action=\"/send_from_form.api\" id=\"sms_form\">\n");
            out.println("    <div><label for=\"name\">Groups (you can select multiple groups):</label></div>\n");
            out.println("    <div><select name=\"group\" id=\"name\" multiple=\"multiple\" size=\"4\" >\n");
            out.println(user_helper.toString());
            out.println("</select> \n");
            out.println(       "     </div>\n" );
            out.println(       "     <br/>\n" );
            out.println(       "     <div><label for=\"message\">SMS TEXT</label></div>\n" );
            out.println(       "     <div><textarea name=\"message\" id=\"message\"></textarea></div><br/>\n" );
            out.println(       "     <div><label for=\"token\">Token</label></div>\n" );
            out.println(       "     <div><input type=\"text\" id=\"token\" name=\"token\" value=\"\"></div><br/>\n" );
            out.println(       "     <div><input type=\"checkbox\" id=\"hidden\" name=\"hidden\" value=\"true\">Send without logging</div><br/>\n" );
            out.println(       "     <div><button type=\"submit\"  id=\"sendBtn\">SEND</button></div>\n" );
            out.println(       "</form>\n" );
            out.println(       "\n" );
            out.println(       "<br/><div id=\"result\"></div>\n");
            out.println(       "<br/>");
            out.println(       "<script src=\"//ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js\"></script>\n" );
            out.println(       "<script>\n" );
            out.println(       "$(document).ready(function() {\n" );
            out.println(       "  $(\"html\").on(\"submit\",\"#sms_form\",function(e){\n" );
            out.println(       "    e.preventDefault();\n" );
            out.println(       "    $(\"#send_form_status\").html('').hide();\n" );
            out.println(       "    var data=$(\"#sms_form\").serialize();\n" );
            out.println(       "    $.post(\"/send_from_form.api\",data,function(res){\n" );
            out.println(       "         alert(res);" );
            out.println(       "      $(\"#send_form_status\").html(res).show();\n" );
            out.println(        "        $(\"#sms_form\")[0].reset();\n" );
            out.println(       "    });\n" );
            out.println(       "  });\n");
            out.println(       "});\n" );
            out.println(       "</script>");             
            printHomeAndBack(out);
      }
        
      private void printSendSmsUser(PrintWriter out){   
            StringBuilder user_helper = new StringBuilder();
            usersPhonebook.forEach((user) -> {
                user_helper.append("<option value=\"").append(user.getUsername()).append("\">").append(user.getUsername()).append("</option>\n");
            });
          
          
             out.print("<h2>Send SMS to USER</h2>\n");
             out.print("<form method=\"post\" action=\"/send_from_form.api\" id=\"sms_form\">\n");
             out.print("    <div><label for=\"name\">Username (you can select multiple users):</label></div>\n");
             out.print("    <div><select name=\"user\" id=\"name\" multiple=\"multiple\" size=\"4\" >\n");
             out.print(user_helper.toString());
             out.print("</select> \n");
             out.print(       "     </div>\n" );
             out.print(       "     <br/>\n" );
             out.print(       "     <div><label for=\"message\">SMS TEXT</label></div>\n" );
             out.print(       "     <div><textarea name=\"message\" id=\"message\"></textarea></div><br/>\n" );
             out.print(       "     <div><label for=\"token\">Token</label></div>\n" );
             out.print(       "     <div><input type=\"text\" id=\"token\" name=\"token\" value=\"\"></div><br/>\n" );
             out.print(       "     <div><input type=\"checkbox\" id=\"hidden\" name=\"hidden\" value=\"true\">Send without logging</div><br/>\n" );
             out.print(       "     <div><button id=\"sendBtn\">SEND</button></div>\n" );
             out.print(       "</form>\n" );
             out.print(       "\n" );
             out.print(       "<script src=\"//ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js\"></script>\n" );
             out.print(       "<script>\n" );
             out.print(       "$(document).ready(function() {\n" );
             out.print(       "  $(\"html\").on(\"submit\",\"#sms_form\",function(e){\n" );
             out.print(       "    e.preventDefault();\n" );
             out.print(       "    $(\"#send_form_status\").html('').hide();\n" );
             out.print(       "    var data=$(\"#sms_form\").serialize();\n" );
             out.print(       "    $.post(\"/send_from_form.api\",data,function(res){\n" );
             out.print(       "         alert(res);" );
             out.print(       "      $(\"#send_form_status\").html(res).show();\n" );
             out.print(        "        $(\"#sms_form\")[0].reset();\n" );
             out.print(       "    });\n" );
             out.print(       "  });\n");
             out.print(       "});\n" );
             out.print(       "</script>"); 
             out.print(       "<br/><div id=\"send_form_status\"></div>\n");
             out.print(       "<br/>");
             
            printHomeAndBack(out);
      }
   
      private void printSendSmsAdmin(PrintWriter out){   
             
             out.print("<h2>Send SMS using phone number</h2>\n");
             out.print("<form method=\"post\" action=\"/send_from_form.api\" id=\"sms_form\">\n");
             out.print("    <div><label for=\"phone\">Phone</label></div>\n");
             out.print("    <div><input type=\"text\" id=\"phone\" name=\"phone\" value=\"+48\"><br></div>\n" );
             out.print("    <br/>\n" );
             out.print("    <div><label for=\"message\">SMS TEXT</label></div>\n" );
             out.print("    <div><textarea name=\"message\" id=\"message\"></textarea></div><br/>\n" );
             out.print("    <div><label for=\"token\">Token</label></div>\n" );
             out.print("    <div><input type=\"text\" id=\"token\" name=\"token\" value=\"\"></div><br/>\n" );
             out.print(       "     <div><input type=\"checkbox\" id=\"hidden\" name=\"hidden\" value=\"true\">Send without logging</div><br/>\n" );
             out.print("    <div><button id=\"sendBtn\">SEND</button></div>\n" );
             out.print("</form>\n" );
             out.print("\n" );
             out.print("<script src=\"//ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js\"></script>\n" );
             out.print("<script>\n" );
             out.print("$(document).ready(function() {\n" );
             out.print("  $(\"html\").on(\"submit\",\"#sms_form\",function(e){\n" );
             out.print("    e.preventDefault();\n" );
             out.print("    $(\"#send_form_status\").html('').hide();\n" );
             out.print("    var data=$(\"#sms_form\").serialize();\n" );
             out.print("    $.post(\"/send_from_form.api\",data,function(res){\n" );
             out.print("         alert(res);" );
             out.print("      $(\"#send_form_status\").html(res).show();\n" );
             out.print("        $(\"#sms_form\")[0].reset();\n" );
             out.print("    });\n" );
             out.print("  });\n");
             out.print("});\n" );
             out.print("</script>"); 
             out.print("<br/><div id=\"send_form_status\"></div>\n");
             out.print("<br/>");
             
             printHomeAndBack(out);
      }
      
       private void printSendSmsTemplate(PrintWriter out){   
           
            out.print("<script>\n");
            out.print("function Change() {\n");
            out.print("  var x = document.getElementById(\"wybor\").value;\n");
            
            String default_group_p="";
            String default_group_s="";
            String default_msg="";
            
            int i=0;
            for (SmsTepmlateApi st : smsTepmlatesApi){
                
                if (i==0){
                    default_group_p=st.printGroups();
                    default_group_s=st.printGroupsPure();
                    default_msg=st.getMessage();
                }
                
                
                out.print(" if (x=='w");
                out.print(i);
                out.print("'){\n");
                out.print("   document.getElementById(\"group_txt\").innerHTML='"); 
                out.print(st.printGroups());
                out.print("';\n   document.getElementById(\"message\").innerHTML='");
                out.print(st.getMessage());
                out.print("';\n");
         
                out.print("");
                out.print("   document.getElementById(\"group\").innerHTML='"); 
                out.print(st.printGroupsPure());
                out.print("';\n");
                
                out.print("}");
                i++;
            }
            out.print("}\n");
            out.print("</script>\n");
           
            out.print("<h2>Send SMS using template</h2>\n");

            out.print("<div><label for=\"wybor\">Template:</label></div>\n");
            out.print("<select name=\"wybor\" id=\"wybor\" onChange=\"Change();\">\n");
            
            i=0;
            for (SmsTepmlateApi st : smsTepmlatesApi){
                out.print(" <option value=\"w" + i + "\">" + st.getName() + "</option>\n");
                i++;
            }
            out.print("</select>\n");
            out.print("<br/><br/>\n");
            
            out.print("<form method=\"post\" action=\"/send_from_form.api\" id=\"sms_form\">\n");
            out.print("    <div><textarea rows=\"1\" hidden=\"true\" name=\"group\" id=\"group\">"+default_group_s+"</textarea></div><br/>\n" );      
            out.print("    <div><label for=\"group_txt\">Groups:</label></div>\n");
            out.print("    <div> <b id='group_txt'>"+default_group_p+"</b><br/><br/>\n" );
            out.print("    <div><label for=\"message\">SMS TEXT</label></div>\n" );
            out.print("    <div><textarea name=\"message\" id=\"message\">"+default_msg+"</textarea></div><br/>\n" );
            out.print("    <div><label for=\"token\">Token</label></div>\n" );
            out.print("    <div><input type=\"text\" id=\"token\" name=\"token\" value=\"\"></div><br/>\n" );
            out.print("    <div><input type=\"checkbox\" id=\"hidden\" name=\"hidden\" value=\"true\">Send without logging</div><br/>\n" );
            out.print("    <div><button id=\"sendBtn\">SEND</button></div>\n" );
            out.print("</form>\n" );
            out.print("\n" );
            out.print("<script src=\"//ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js\"></script>\n" );
            out.print("<script>\n" );
            out.print("$(document).ready(function() {\n" );
            out.print("  $(\"html\").on(\"submit\",\"#sms_form\",function(e){\n" );
            out.print("    e.preventDefault();\n" );
            out.print("    $(\"#send_form_status\").html('').hide();\n" );
            out.print("    var data=$(\"#sms_form\").serialize();\n" );
            out.print("    $.post(\"/send_from_form.api\",data,function(res){\n" );
            out.print("         alert(res);" );
            out.print("      $(\"#send_form_status\").html(res).show();\n" );
            out.print("        $(\"#sms_form\")[0].reset();\n" );
            out.print("    });\n" );
            out.print("  });\n");
            out.print("});\n" );
            out.print("</script>\n"); 
            out.print("<br/><div id=\"send_form_status\"></div>\n");
            out.print("<br/>\n");
            
            printHomeAndBack(out);
            
      }



      public String getPhoneForUser(String _user) {
            String ret = "default";

            for (UserPhonebook user : usersPhonebook) {
              if (user.getUsername().equalsIgnoreCase(_user)) {
                ret = user.getPhone();
                break;
              }
            }

            return ret;
      }

      public String getUserForPhone(String _phone)  {
            String ret = "default";

            for (UserPhonebook user : usersPhonebook) {
              if (user.getPhone().equalsIgnoreCase(_phone)) {
                ret = user.getUsername();
                break;
              }
            }

            return ret;
      }

      public boolean isActiveUserPhone(String phone) {
            boolean ret = false;
            String _user = getUserForPhone(phone);
           
            if (activeUsers.getActiveUsers().containsValue(_user)) {
                 ret = true;
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
      
    public String uptimeHumanReadable(Duration duration){
        StringBuilder upt = new StringBuilder ();
        upt.append("Days: ").append(duration.toDays()).append(", hours: ").append((duration.toHours()-duration.toDays()*24)).
                append(", min: ").append((duration.toMinutes()-duration.toHours()*60)).append(", sec: ").append((duration.toMillis()/1000-duration.toMinutes()*60));
        return upt.toString();

    }
    
    private String replacePolish(String sdecoded){
            sdecoded = sdecoded.replaceAll("ą", "a");
            sdecoded = sdecoded.replaceAll("Ą", "A");

            sdecoded = sdecoded.replaceAll("ć", "c");
            sdecoded = sdecoded.replaceAll("Ć", "C");

            sdecoded = sdecoded.replaceAll("ę", "e");
            sdecoded = sdecoded.replaceAll("Ę", "E");

            sdecoded = sdecoded.replaceAll("ł", "l");
            sdecoded = sdecoded.replaceAll("Ł", "L");

            sdecoded = sdecoded.replaceAll("ń", "n");
            sdecoded = sdecoded.replaceAll("Ń", "N");

            sdecoded = sdecoded.replaceAll("ó", "o");
            sdecoded = sdecoded.replaceAll("Ó", "O");

            sdecoded = sdecoded.replaceAll("ś", "s");
            sdecoded = sdecoded.replaceAll("Ś", "S");

            sdecoded = sdecoded.replaceAll("Ź", "Z");
            sdecoded = sdecoded.replaceAll("ź", "z");

            sdecoded = sdecoded.replaceAll("Ż", "Z");
            sdecoded = sdecoded.replaceAll("ż", "z");
            
            return sdecoded;
    }
    
    private String replaceURL(String postData){
          
         try {
              postData = URLDecoder.decode(postData, "UTF-8");
          } catch (UnsupportedEncodingException ex) {
              logger.error("replaceURL decoder");
          }
              
          postData = postData.replaceAll("%C4%84", "A");
          postData = postData.replaceAll("%C4%85", "a");
          postData = postData.replaceAll("%C4%87", "C");
          postData = postData.replaceAll("%C4%88", "c");
          postData = postData.replaceAll("%C4%98", "E");
          postData = postData.replaceAll("%C4%99", "e");
          postData = postData.replaceAll("%C5%81", "L");
          postData = postData.replaceAll("%C5%82", "l");

          postData = postData.replaceAll("%C5%83", "N");
          postData = postData.replaceAll("%C5%84", "n");

          postData = postData.replaceAll("%C3%B3", "o");
          postData = postData.replaceAll("%C3%93", "O");

          postData = postData.replaceAll("%C5%9A", "S");
          postData = postData.replaceAll("%C5%9B", "s");
          postData = postData.replaceAll("%C5%B9", "Z");
          postData = postData.replaceAll("%C5%BA", "z");
          postData = postData.replaceAll("%C5%BB", "Z");
          postData = postData.replaceAll("%C5%BC", "z");
          postData = postData.replaceAll("%20", " ");

         
          postData = postData.replaceAll("%5B", "[");
          postData = postData.replaceAll("%5C", "\\");
          postData = postData.replaceAll("%5D", "]");
          postData = postData.replaceAll("%21", "!");
          postData = postData.replaceAll("%22", "\"");
          postData = postData.replaceAll("%23", "#");
          postData = postData.replaceAll("%24", "$");
          postData = postData.replaceAll("%28", "(");
          postData = postData.replaceAll("%29", ")");
          postData = postData.replaceAll("%40", "@");
          postData = postData.replaceAll("%3F", "?");
          postData = postData.replaceAll("%25", "%");

          return postData;
          
                
    }
    
    
    private boolean isAdmin( PrintWriter out, boolean isGet ){
        if (!isAdmin){
             if (isGet) {
                 String data="<h1>401 UNAUTHORIZED</h1>";
                 String response = "HTTP/1.1 401 UNAUTHORIZED\r\n" +
                    "Content-Length: "+data.length()+"\r\n" +
                    "Content-Type: text/html\r\n\r\n" +  data;
                 out.println(response);
                 out.println("<b>Sorry, you are not allowed to access this page.</b><br/>");
             }
             else out.println("Sorry, you are not allowed to access this page.\n");
        } 
        
        return isAdmin;
    }  
    
    private boolean isApi( PrintWriter out, boolean isGet ){
        if (!isApi){
             if (isGet) {
                 String data="<h1>401 UNAUTHORIZED</h1>";
                 String response = "HTTP/1.1 401 UNAUTHORIZED\r\n" +
                    "Content-Length: "+data.length()+"\r\n" +
                    "Content-Type: text/html\r\n\r\n" +  data;
                 out.println(response);
                 out.println("<b>Sorry, you are not allowed to access this page.</b><br/>");
             }
             else out.println("Sorry, you are not allowed to access this page.\n");
        } 
        
        return isApi;
    }  
    
    
    private void printJVM(PrintWriter out)  {
        Runtime runtime = Runtime.getRuntime(); 
        
        
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMem = allocatedMemory - freeMemory;
        long totalMem = runtime.totalMemory();
        long mb=1024;
        
        out.println("<b><h3>Heap utilization statistics [MB] </h3>" );
        out.println(" </b>");
        out.println("Total Memory: " + totalMem / mb);
        out.println("<br/>");
        out.println("Free Memory:  " + freeMemory / mb);
        out.println("<br/>");
        out.println("Used Memory:  " + usedMem / mb);
        out.println("<br/>");
        out.println("Max Memory:   " + maxMemory / mb);
        out.println("<br/>");
       
        out.println("<b><h3>SystemMXBean statistics</h3></b>" );
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
         for (Method method : operatingSystemMXBean.getClass().getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.getName().startsWith("get") 
                && Modifier.isPublic(method.getModifiers())) {
                    Object value;
                try {
                    value = method.invoke(operatingSystemMXBean);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    value = e;
                } // try
                out.println(method.getName() + " = " + value);
                out.println("<br/>");
            } // if
          } // for
        
    }
    
    private void printAllThreads(PrintWriter out){
       out.print("<h2>ALL THREADS</h2>\n");  
       ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
       int noThreads = currentGroup.activeCount();
       Thread[] lstThreads = new Thread[noThreads];
       currentGroup.enumerate(lstThreads);
       
        out.println("<style>\n table, th, td {\n border: 1px solid black;\n border-collapse: collapse;\n }\n th, td {\n padding: 5px;\n");
        out.println(" text-align: left;   \n}\n </style>");  
          
        
        out.println("<table style=\"width:50%\">");      
        out.println("<tr>\n <th>ID</th>\n <th>Thread ID</th>\n  <th>Thread name</th>\n  <th>Thread state</th>\n  <th>Thread priority</th>\n </tr>");
       
      
        for (int i = 0; i < noThreads; i++){
                     
                 out.println("<tr>"); 
                 
                 out.println("<td>");
                 out.print(i);
                 out.println("</td>"); 
                 
                 out.println("<td>");
                 out.print(lstThreads[i].getId());
                 out.println("</td>"); 
                 
                 
                 out.println("<td>");
                 out.print(lstThreads[i].getName());
                 out.println("</td>"); 
                 
                 out.println("<td>");
                 out.print(lstThreads[i].getState());
                 out.println("</td>"); 
                 
                 out.println("<td>");
                 out.print(lstThreads[i].getPriority());
                 out.println("</td>"); 
               
                 out.println("</tr>"); 
                 
        }
        
       
        out.println("</table>");
      
      
    }
    
  
 
      
}