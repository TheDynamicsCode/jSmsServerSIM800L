/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms;

import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author dominik
 */
public class ATCommandsManager {
   
    private static boolean at_lock_write = false; 
    private static long locked_time_write=0;
    private static UUID current_uuid;
    private static boolean wasTimeout=false;
  
    static final Logger logger =  LogManager.getLogger("biz.szydlowski.sms.ATCommandsManager");
   
    public static void setCurrentUUID(UUID _uuid){
        current_uuid = _uuid;
    }
   
    public static UUID getCurrentUUID(){
        return current_uuid;
    }
    
    public static void lockWrite(){
        logger.debug("lockWrite");
        at_lock_write=true;
        locked_time_write = System.currentTimeMillis();
    }  
      
   
    public synchronized static void unlockWrite(){
        logger.debug("unlockWrite");
        at_lock_write=false;
    }  
    
    
    
    public synchronized static boolean isWriteLockedOrTimeout(){
        if (at_lock_write){
           if ( (System.currentTimeMillis()-locked_time_write) > 10000){
               logger.warn("LOCKED WRITE timeout for current_uuid "  + current_uuid);
               wasTimeout=true;
               at_lock_write= false;
          } else {
               wasTimeout=false;
          }
        } else {
            wasTimeout=false;
        }
     
        return at_lock_write;
    }   
    
    public synchronized static boolean isWriteLockedStatus(){
        return at_lock_write;
    }        
       
    public synchronized static boolean wasTimeout(){
        return wasTimeout;
    }        
       
        
}
