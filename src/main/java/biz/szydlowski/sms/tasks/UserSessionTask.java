/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.tasks;

import biz.szydlowski.sms.Messaging;
import static biz.szydlowski.sms.WorkingObjects.archiveToFile;
import static biz.szydlowski.sms.WorkingObjects.lastUserSession;
import static biz.szydlowski.sms.WorkingObjects.usersApi;
import biz.szydlowski.sms.activeUsers;
import biz.szydlowski.sms.api.UserApi;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Dominik
 */
public class UserSessionTask extends TimerTask {
    
           static final Logger logger =  LogManager.getLogger(UserSessionTask.class);
            
            @Override
            public void run() { 
                  
                  lastUserSession = System.currentTimeMillis();
                 
                  
                  Iterator<Map.Entry<Instant, String> >  iterator =  activeUsers.getActiveUsers().entrySet().iterator();
  
                   // Iterate over the HashMap
                   while (iterator.hasNext()) {
                        
                        Map.Entry<Instant, String>  entry = iterator.next();
                        if (Duration.between(Instant.now(), entry.getKey()).toMillis() < 0){
                               
                         
                               logger.debug("logout automatically - timeout " +entry.getValue());                                 
                               String phone = getPhoneForUserAPI(entry.getValue());
                             
                               Messaging.addSMStoQueue(getPhoneForUserAPI(entry.getValue()), "System logout user " + entry.getValue() + " automatically.", false);
                               archiveToFile.addToRepository("audit.out", "Logout/A\t" + phone); 
                               
                               iterator.remove();
                            
                        } 
                  }
                
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
      
            
     }
