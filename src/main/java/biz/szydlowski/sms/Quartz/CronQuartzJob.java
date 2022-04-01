/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.Quartz;

import biz.szydlowski.sms.Messaging;
import biz.szydlowski.sms.activeUsers;
import static biz.szydlowski.sms.WorkingObjects.usersPhonebook;
import biz.szydlowski.sms.api.CronApi;
import biz.szydlowski.sms.api.UserPhonebook;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
public class CronQuartzJob implements InterruptableJob {
   
   static final Logger logger =  LogManager.getLogger("biz.szydlowski.sms.CronQuartzJob");
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

                CronApi cronApi = (CronApi) jdMap.get("cronApi");
                
                List<String> unique_users =  new ArrayList<>();
                        
                if (cronApi.isUser()){
                     unique_users = cronApi.getUsersOrGroups();
                } else if (cronApi.isGroup()){
                    List<String> split_group =  cronApi.getUsersOrGroups();
                    boolean isUniq=true;

                    for (UserPhonebook u: usersPhonebook){ //kontakty

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
       

                for (String send_user : unique_users){
                  
                    String sendTo = getPhoneForUser(send_user);

                    if (sendTo.equalsIgnoreCase("default")){
                        logger.error(new StringBuilder().append("ERROR: Unknown username ").append(send_user).append("\n").toString());
                    } else {
                        
                        if ( cronApi.isSendOnlyToLoginUsers()){
                            if (activeUsers.isActiveUser(send_user)){
                                UUID uuid = Messaging.addSMStoQueue(sendTo,  cronApi.getMessage(), false) ;
                                logger.info(new StringBuilder().append("SMS send to ").append(send_user).append(" (").append(sendTo).append(")<br/>\n").toString());
                                logger.info(new StringBuilder().append("Add sms message ").append(cronApi.getMessage()).append(" to queue ").toString());
                                logger.info(new StringBuilder().append("with uuid ").append(uuid).append("<br/>\n").toString());
                           } else {
                                logger.info("User " + send_user + " is not login.");
                            }
                        } else {
                            UUID uuid = Messaging.addSMStoQueue(sendTo,  cronApi.getMessage(), false) ;
                            logger.info(new StringBuilder().append("SMS send to ").append(send_user).append(" (").append(sendTo).append(")<br/>\n").toString());
                            logger.info(new StringBuilder().append("Add sms message ").append(cronApi.getMessage()).append(" to queue ").toString());
                            logger.info(new StringBuilder().append("with uuid ").append(uuid).append("<br/>\n").toString());
                  
                        }

                    }
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
 
   
}
