/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.tasks;


import static biz.szydlowski.sms.WorkingObjects.TOKEN;
import static biz.szydlowski.sms.WorkingObjects.TOKEN_OLD;
import java.util.Random;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Dominik
 */
public class TokenTask extends TimerTask {
    
            static final Logger logger =  LogManager.getLogger(TokenTask.class);
            private boolean fisrtTokenTask=true;
            
            
            @Override
            public void run() {
                logger.debug("Generate new tokens....");
                 
                 if ( fisrtTokenTask){
                      fisrtTokenTask=false;
                     TOKEN=generateToken();
                     TOKEN_OLD=generateToken();
                      
                 } else {
                     TOKEN_OLD = TOKEN ;
                     TOKEN=generateToken();
                 }
                 
                 
            }
            
            private String generateToken(){
                 // Generate random id, for example 283952-V8M32
                char[] chars = "0123456789".toCharArray();
                Random rnd = new Random();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 5; i++)
                    sb.append(chars[rnd.nextInt(chars.length)]);

                return sb.toString();
            }
    
    }    