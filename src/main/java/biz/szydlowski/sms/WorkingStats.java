/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms;

import java.time.Duration;
import java.time.Instant;

/**
 *
 * @author szydlowskidom
 */
public class WorkingStats {
    
    private static Instant start;
    private static long sms_send_count=0;
    private static long sms_read_count=0;
    private static long error_at_count=0;
    private static long ok_at_count=0; 
    private static long at_send_commands = 0;
    private static int javaUsedMemory = 0;
   
    public synchronized  static void start(){
        start = Instant.now();
    }  
   
    public static void OkATCountPlus(){
        ok_at_count++;
    }   
  
    public static void ErrorATCountPlus(){
        error_at_count++;
    } 
       
     
    public static void AtSendCommandsPlus(){
        at_send_commands++;
    } 
            
    public static void smsSendCountPlus(){
        sms_send_count++;
    } 
    
    public static void smsReadCountPlus(){
        sms_read_count++;
    } 
    
        
    public static Duration getUptime(){
          return Duration.between(start, Instant.now());
    } 
    
    public static Instant getStartTime(){
          return start;
    }  
    
    public static long getSmsSendCount(){
       return  sms_send_count;
    } 
    
    public static long getSmsReadCount(){
       return  sms_read_count;
    } 
     
    public static long getOkATCount(){
       return ok_at_count;
    }  
    
    public static long getErrorATCount(){
       return error_at_count;
    }  

  
    public static long getAtSendCommands(){
       return at_send_commands;
    } 
    
    public static int getJavaUsedMemory(){
        return javaUsedMemory ;
    } 
    
    public static void setJavaUsedMemory(int mem){
         javaUsedMemory =mem ;
    }  
    
 
}
