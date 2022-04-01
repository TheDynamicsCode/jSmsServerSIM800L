/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.Quartz;

import biz.szydlowski.sms.Messaging;
import biz.szydlowski.sms.activeUsers;
import static biz.szydlowski.sms.WorkingObjects.archiveToFile;
import static biz.szydlowski.sms.WorkingObjects.usersApi;
import biz.szydlowski.sms.api.AutoLogCronApi;
import biz.szydlowski.sms.api.UserApi;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.UnableToInterruptJobException;

/**
 *
 * @author Dominik
 */
public class AutoLogQuartzJob implements InterruptableJob {
   
   static final Logger logger =  LogManager.getLogger(AutoLogQuartzJob.class);
   private volatile Thread thisThread;
   private JobKey jobKey = null;
   private volatile boolean isJobInterrupted = false;

   
   @Override
   public void execute(JobExecutionContext jeContext)   throws JobExecutionException   {
             this.thisThread = Thread.currentThread();

             this.jobKey = jeContext.getJobDetail().getKey();
             logger.info("Job " + this.jobKey + " executing at " + new Date());
             try  {
              
                JobDataMap jdMap = jeContext.getJobDetail().getJobDataMap();

                AutoLogCronApi autoLogCronApi = (AutoLogCronApi ) jdMap.get("autoLogCronApi");
                
                List<String> unique_users =  new ArrayList<>();
                        
                if (autoLogCronApi.isUser()){
                     unique_users = autoLogCronApi.getUsersOrGroups();
                } else if (autoLogCronApi.isGroup()){
                    List<String> split_group = autoLogCronApi.getUsersOrGroups();
                    boolean isUniq=true;

                    for (UserApi u: usersApi){ 
                        
                        String user = u.getUsername();

                        for (String gr : u.getGroups()){
                             for (String sgroup : split_group){
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
                } else {
                    return;
                }
               
               String type = autoLogCronApi.getActionType();
               String number="";
               long timeout_login = 60;
                
              for (String action_user : unique_users){
                 
                  StringBuilder msg = new StringBuilder ();
                           
                  for (UserApi u : usersApi){
                     if (u.getUsername().equals(action_user)){
                       timeout_login = u.getTimeoutInMinutes();
                       number=u.getPhone();
                     }
                  }
                  if (type.toLowerCase().startsWith("login.")) {
                      
                      timeout_login = 60;
                      try {                                        
                           timeout_login=Long.parseLong(type.toLowerCase().replace("login.", "").replaceAll(" ", ""))*60;
                      } catch (Exception h){                                                       
                      }        
                      
                      if (activeUsers.isActiveUser(action_user)){
                           activeUsers.deleteUser(action_user);
                           if (activeUsers.addUser(action_user, timeout_login)){
                               msg.append("User ").append(action_user).append(" was re-login successfully by EmbeddedCron.\n");
                               msg.append("Auto logout time: ").append(activeUsers.getLogoutTimeUsers(action_user)).append(".\n");
                               archiveToFile.addToRepository("audit.out", "Login/SA/Cron\t" + action_user); 
                           } else {
                               msg.append("Log in failed ").append(action_user).append("\n");
                           }
                             
                       } else {                      
                        
                           if (activeUsers.addUser(action_user, timeout_login)){
                               msg.append("User ").append(action_user).append(" was log in successfully by EmbeddedCron.\n");
                               msg.append("Auto logout time: ").append(activeUsers.getLogoutTimeUsers(action_user)).append(".\n");
                               archiveToFile.addToRepository("audit.out", "Login/SA/Cron\t" + action_user); 
                           } else {
                               msg.append("Log in failed ").append(action_user).append("\n");
                           }
                       }                      
                          
                  } else if (type.equals("login")){
                   
                       if (activeUsers.isActiveUser(action_user)){
                           activeUsers.deleteUser(action_user);
                           if (activeUsers.addUser(action_user, timeout_login)){
                               msg.append("User ").append(action_user).append(" was re-login successfully by EmbeddedCron\n");
                               msg.append("Auto logout time: ").append(activeUsers.getLogoutTimeUsers(action_user)).append(".\n");
                               archiveToFile.addToRepository("audit.out", "Login/SA/Cron\t" + action_user); 
                           } else {
                               msg.append("Log in failed ").append(action_user).append("\n");
                           }
                             
                       } else {                      
                        
                           if (activeUsers.addUser(action_user, timeout_login)){
                               msg.append("User ").append(action_user).append(" was log in successfully by EmbeddedCronn");
                               msg.append("Auto logout time: ").append(activeUsers.getLogoutTimeUsers(action_user)).append(".\n");
                               archiveToFile.addToRepository("audit.out", "Login/SA/Cron\t" + action_user); 
                           } else {
                               msg.append("Log in failed ").append(action_user).append("\n");
                           }
                       }
                   } else if (type.equals("logout")) {

                           if (!activeUsers.isActiveUser(action_user)){
                                 //msg.append("User ").append(action_user).append(" is not login (autolog-cron)");
                           } else {
                               if (activeUsers.deleteUser(action_user)){
                                   msg.append("User ").append(action_user).append(" was log out successfully cron\n");
                                   archiveToFile.addToRepository("audit.out", "Logout/SA/Cron\t" + action_user);
                               } else {
                                   msg.append("Log out failed ").append(action_user);
                               }
                           } 
                           
                   }
                 
                   logger.debug("MSG to send " + msg.toString());
                   if (msg.length()>0) Messaging.addSMStoQueue(number, msg.toString(), false);
                   
               }
                
               
            

            } catch (Exception e)   {
                  logger.info("--- Error in job ! ----");       
                  Thread.currentThread().interrupt();

                  JobExecutionException e2 = new JobExecutionException(e);

                  e2.refireImmediately();
                  throw e2;
            }
            finally     {
              if (this.isJobInterrupted) {
                 logger.info("Job " + this.jobKey + " did not complete");
              } else {
                 logger.info("Job " + this.jobKey + " completed at " + new Date());
               }
            }
  }
    
   @Override
   public void interrupt() throws UnableToInterruptJobException {
         logger.info("Job " + this.jobKey + "  -- INTERRUPTING --");
         this.isJobInterrupted = true;
         if (this.thisThread != null) {
             this.thisThread.interrupt();
         }
   }
   

   
}