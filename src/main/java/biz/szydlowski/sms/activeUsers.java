/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms;

import static biz.szydlowski.sms.WorkingObjects.usersApi;
import biz.szydlowski.sms.api.UserApi;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static jdk.nashorn.internal.objects.NativeArray.map;


/**
 *
 * @author szydlowskidom
 */
public class activeUsers {
    
    //private static final List<String> activeUsers = new ArrayList<>();
   // private static final List<Instant> logoutTime = new ArrayList<>();
    private static final Map<Instant, String> mapActiveUsers = new HashMap<>();
    
    public static boolean addUser(String alias){
        boolean found=false;
        
        if (!isActiveUser(alias)) {            
            long timeout_login = 60;
            for (UserApi u : usersApi){
               if (u.getUsername().equalsIgnoreCase(alias)){
                   timeout_login = u.getTimeoutInMinutes();
                   found=true;
                   break;
               }
            }  
            if (found){
                found=addUser(alias, timeout_login);
            }
            return found;
        } else return false;
    }  
     
    public static boolean addUser(String alias, long minutesToAdd){
        if (!isActiveUser(alias)) {
            mapActiveUsers.put(Instant.now().plusSeconds(minutesToAdd*60), alias);
           // activeUsers.add(alias);
           // logoutTime.add(Instant.now().plusSeconds(minutesToAdd*60));
            return true;
        } else return false;
    }  
    
    public static boolean deleteUser(String alias){
         boolean ret=false;
         /*for (int i=0; i<activeUsers.size(); i++){
           if (activeUsers.get(i).equals(alias)){
               activeUsers.remove(i); 
               logoutTime.remove(i);
               ret=true;
               break;
           }
        }*/
          // Get the iterator over the HashMap
        Iterator<Map.Entry<Instant, String> >  iterator =  mapActiveUsers.entrySet().iterator();
  
        // Iterate over the HashMap
        while (iterator.hasNext()) {
  
            // Get the entry at this iteration
            Map.Entry<Instant, String>  entry = iterator.next();
  
            // Check if this value is the required value
            if (alias.equals(entry.getValue())) {  
                // Remove this entry from HashMap
                iterator.remove();
                ret=true;
            }
        } 
         
        return ret;
    }   
    
   /* public static boolean deleteUser(int id){
         boolean ret=false;
         
         if (id<activeUsers.size()){
             activeUsers.remove(id); 
             logoutTime.remove(id);
             ret=true;
         }
        
        return ret;
    }*/      
    
    
    public static boolean isActiveUser(String u){
        boolean ret=false;
        if (mapActiveUsers.containsValue(u)) {
            ret=true;
        }
        
        /*for (int i=0; i<activeUsers.size(); i++){
           if (activeUsers.get(i).equals(u)){
               ret=true;
               break;
           }
        }*/
        return ret;
    }   
    
    public static Map<Instant, String> getActiveUsers(){
        return  mapActiveUsers;
    }   
    
   /* public static List<Instant> getLogoutTimeUsers(){
        return logoutTime;
    }*/ 
    
    
    public static synchronized String getLogoutTimeUsers(String user){
       String logouttime="not login";
                          
      /* for (int id=logoutTime.size()-1; id>=0; id--){
          if (activeUsers.size()<id){ //bug 17062021
            logouttime="internal error";  
          } else  if (activeUsers.get(id).equals(user) ){             
            ZonedDateTime jpTime = logoutTime.get(id).atZone(ZoneId.systemDefault());
            logouttime=""+jpTime;
            break;
         }
                      
      }*/
      
        Iterator<Map.Entry<Instant, String> >  iterator =  mapActiveUsers.entrySet().iterator();
  
        // Iterate over the HashMap
        while (iterator.hasNext()) {
  
            // Get the entry at this iteration
            Map.Entry<Instant, String>  entry = iterator.next();
  
            // Check if this value is the required value
            if (user.equals(entry.getValue())) {  
                   ZonedDateTime jpTime = entry.getKey().atZone(ZoneId.systemDefault());
                   logouttime=""+jpTime;
            }
        } 
      
       return logouttime;
       
    }   
   
}
