/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.tasks;

import biz.szydlowski.sms.ATCommandsManager;
import static biz.szydlowski.sms.JDaemonSms.statistics;
import biz.szydlowski.sms.Messaging;
import static biz.szydlowski.sms.Messaging.sms_sender;
import biz.szydlowski.sms.Version;
import static biz.szydlowski.sms.WorkingObjects.SMS_ExpireDate;
import static biz.szydlowski.sms.WorkingObjects.error_sms_expire;
import static biz.szydlowski.sms.WorkingObjects.lastMaintenance;
import static biz.szydlowski.sms.WorkingObjects.md5SMSHeader;
import static biz.szydlowski.sms.WorkingObjects.usersApi;
import static biz.szydlowski.sms.WorkingObjects.warn_sms_expire_trigger;
import biz.szydlowski.sms.WorkingStats;
import biz.szydlowski.sms.activeUsers;
import static biz.szydlowski.utils.tasks.TasksWorkspace.absolutePath;
import biz.szydlowski.zabbixmon.DataObject;
import biz.szydlowski.zabbixmon.SenderResult;
import biz.szydlowski.zabbixmon.ZabbixSender;
import biz.szydlowski.zabbixmon.ZabbixServer;
import biz.szydlowski.zabbixmon.ZabbixServerApi;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Dominik
 */
public class MaintenanceTask extends TimerTask {
    
            static final Logger logger =  LogManager.getLogger( MaintenanceTask.class);
            ZabbixServerApi zabbixServerApi = null; 
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss"); 
             
            int tick=0;
            int mb = 1024 * 1024; 
            Runtime runtime = Runtime.getRuntime();
            boolean firstRun=true;
            boolean cleanSmsMD5List=true;
            
            long total_uptime_sec=0L;
            long total_at_send=0L;
            long total_at_error=0L;
            long total_at_ok=0L;
            long total_sms_reads=0L;
            long total_sms_sends=0L;
            int totalMaxUsedMem=0;
            int sessionMaxUsedMem=0; 
            
            long maxMemory ;
            long allocatedMemory ;
            long freeMemory ;
            long usedMem ;
            long totalMem ;
            
            @Override
            public void run() {
                   lastMaintenance=System.currentTimeMillis();
                   
                   if (firstRun){
                        zabbixServerApi = new ZabbixServer(absolutePath +"setting/zabbix-server.xml").getZabbixServerApiList().get(0);
            
                       
                       
                        total_uptime_sec = statistics.getTotalUptimeSec();
                        total_at_send = statistics.getTotalAtSend();
                        total_at_error = statistics.getTotalAtError();
                        total_at_ok = statistics.getTotalAtOK();
                        total_sms_reads = statistics.getTotalSmsReads();
                        total_sms_sends = statistics.getTotalSmsSends();
                        totalMaxUsedMem = statistics.getMaxJavaUsedMemory();
                        firstRun=false;
                   }
                   
                   if (zabbixServerApi.isEnabled()){
                         if (tick%4==0){
                             List<DataObject> dataObject_s = new ArrayList<>();

                             for (int i=0; i<8; i++){

                                 DataObject dataObject = new DataObject();
                                 dataObject.setHost(zabbixServerApi.getMaintenance_host());
                                 switch (i) {
                                     case 0:
                                         dataObject.setKey("jsms.server.boottime");
                                         dataObject.setValue(""+WorkingStats.getUptime().getSeconds());
                                         break;
                                     case 1:
                                         dataObject.setKey("jsms.at.send");
                                         dataObject.setValue(""+WorkingStats.getAtSendCommands());
                                         break;
                                     case 2:
                                         dataObject.setKey("jsms.at.success");
                                         dataObject.setValue(""+WorkingStats.getOkATCount());
                                         break;
                                     case 3:
                                         dataObject.setKey("jsms.at.errors");
                                         dataObject.setValue(""+WorkingStats.getErrorATCount());
                                         break;
                                     case 4: 
                                         dataObject.setKey("jsms.sms.reads");
                                         dataObject.setValue(""+WorkingStats.getSmsReadCount());
                                         break;
                                     case 5:
                                         dataObject.setKey("jsms.sms.sends");
                                         dataObject.setValue(""+WorkingStats.getSmsSendCount());
                                         break;
                                     case 6:
                                         dataObject.setKey("jsms.version");
                                         dataObject.setValue(Version.getAgentVersion());
                                         break;
                                     case 7:
                                         dataObject.setKey("java.used.memory");
                                         dataObject.setValue(""+WorkingStats.getJavaUsedMemory());
                                         break;    
                                          
                                     default:
                                         break;
                                 }

                                 dataObject.setState(0);
                                 dataObject.setClock(System.currentTimeMillis()/1000);

                                 dataObject_s.add(dataObject);
                             }


                            ZabbixSender zabbixSender = new ZabbixSender(zabbixServerApi.getHost(), zabbixServerApi.getPort(), zabbixServerApi.getConnectTimeout(), zabbixServerApi.getSocketTimeout());
                            SenderResult result = zabbixSender.send(dataObject_s);

                            logger.info("result:" + result);
                            if (result.success()) {
                                logger.info("send success.");
                            } else {
                                logger.info("send fail!");
                            }

                            dataObject_s.clear();
                            
                        }

                   } else {
                         logger.info("zabbixServerApi is not Enabled");
                   }
                     
                     for (int i=sms_sender.size()-1; i>=0; i--){
                        if (System.currentTimeMillis()-sms_sender.get(i) > 3600000){ //1 hour
                             logger.debug("sms_sender remove " + i + " " + sms_sender.get(i));      
                             sms_sender.remove(i);
                        }
                        
                    }
                   
                   Date date = new Date();
                                     
                   if(SMS_ExpireDate.before(date)) {
                           if (!error_sms_expire){
                               System.out.println("SMS_EXPIRE_DATE DATE EXPIRED");
                               System.err.println("SMS_EXPIRE_DATE DATE EXPIRED");
                               logger.fatal("****** SMS_EXPIRE_DATE DATE EXPIRED ******");
                           }
                           error_sms_expire=true;
                   } else {
                       if (warn_sms_expire_trigger){ 
                           Date date2 = new Date();
                           date2.setTime(SMS_ExpireDate.toInstant().minusMillis(86400000).toEpochMilli());
                           //System.out.println(date2);
                           if(date2.before(date)) {
                               logger.warn("****** SMS_EXPIRE_DATE DATE EXPIRED 1 DAY ******");
                               usersApi.stream().filter((user) -> (user.isActiveAtStartup() && user.isAdmin())).map((user) -> user.getPhone()).forEachOrdered((phone) -> {
                                   Messaging.addSMStoQueue(phone, "WARNING - SMS_EXPIRE_DATE TOMORROW!!! " + SMS_ExpireDate, false);
                               });
                                warn_sms_expire_trigger=false;
                           }
                       }
                   } 
                    
                
                   
                   if (LocalDateTime.now().getHour()==3 && LocalDateTime.now().getMinute()<20){
                       if (cleanSmsMD5List){
                           logger.info("CLEAN SMS-MD5 HEADER LIST, size " + md5SMSHeader.size());
                           md5SMSHeader.clear();
                           cleanSmsMD5List=false;
                       }
                   } else if (!cleanSmsMD5List) {
                       cleanSmsMD5List=true;
                   }
                     
                   maxMemory = runtime.maxMemory();
                   allocatedMemory = runtime.totalMemory();
                   freeMemory = runtime.freeMemory();
                   usedMem = allocatedMemory - freeMemory;
                   totalMem = runtime.totalMemory();
                   WorkingStats.setJavaUsedMemory( (int) usedMem / mb );
                   
                    if (tick==50){
                        logger.info("***** Heap utilization statistics [MB] *****");
                        // available memory
                        logger.info("Total Memory: " + totalMem / mb);
                        // free memory
                        logger.info("Free Memory: " + freeMemory / mb);
                        // used memory
                        logger.info("Used Memory: " + usedMem / mb);
                        // Maximum available memory
                        logger.info("Max Memory: " + maxMemory / mb);
                       
                        tick=0;
                        
                       // System.gc();
                    }
               
                    statistics.setProperty("java.total.memory.mb",  ""+totalMem / mb);
                    statistics.setProperty("java.free.memory.mb",  ""+freeMemory / mb);
                    statistics.setProperty("java.used.memory.mb",  ""+usedMem / mb);
                    statistics.setProperty("java.max.memory.mb",  ""+maxMemory / mb);
                   
                    statistics.setProperty("active.threads",  ""+Thread.activeCount());
                    statistics.setProperty("active.users",  ""+activeUsers.getActiveUsers().size());  
                 
                    long tmp = total_uptime_sec + WorkingStats.getUptime().toMillis()/1000;
                    statistics.setProperty("session.uptime_sec",  ""+WorkingStats.getUptime().toMillis()/1000);
                    statistics.setProperty("total.uptime_sec",  ""+tmp);
                
        
                    tmp = total_at_send + WorkingStats.getAtSendCommands();
                    statistics.setProperty("session.at.sends",  ""+WorkingStats.getAtSendCommands());
                    statistics.setProperty("total.at.sends",  ""+tmp);
                    
                    tmp = total_at_error  + WorkingStats.getErrorATCount();                
                    statistics.setProperty("session.at.error",  ""+WorkingStats.getErrorATCount());
                    statistics.setProperty("total.at.error",  ""+tmp);
                    
   
                    tmp = total_at_ok  + WorkingStats.getOkATCount();      
                    statistics.setProperty("session.at.ok",  ""+WorkingStats.getOkATCount());
                    statistics.setProperty("total.at.ok",  ""+tmp);
                    
        
                    tmp = total_sms_reads + WorkingStats.getSmsReadCount();               
                    statistics.setProperty("session.sms.reads",  ""+WorkingStats.getSmsReadCount());
                    statistics.setProperty("total.sms.reads",  ""+tmp);
                   
              
                    tmp = total_sms_sends + WorkingStats.getSmsSendCount(); 
                    statistics.setProperty("session.sms.sends",  ""+WorkingStats.getSmsSendCount());
                    statistics.setProperty("total.sms.sends",  ""+tmp);
                 
                    statistics.setProperty("messaging.size",  ""+ Messaging.queue_uuid.size());
                    
                    if (totalMaxUsedMem<usedMem) {
                       totalMaxUsedMem=(int)usedMem/mb;
                        statistics.setProperty("total.java.max.used.memory.mb", ""+totalMaxUsedMem);
                    }
                   
                    if (sessionMaxUsedMem<usedMem){
                        sessionMaxUsedMem=(int)usedMem/mb; 
                        statistics.setProperty("session.java.max.used.memory.mb", ""+sessionMaxUsedMem);
                    }
                                      
                    statistics.setProperty("heartbeat.time", sdf.format(new Date()));
                    statistics.writeStats();
                    
                    tick++;
                    
                    if (Messaging.at_done.size()>9999) {
                        logger.info("Clear messaging: " + Messaging.at_done.size());
                        Messaging.clearMessaging();
                        ATCommandsManager.unlockWrite();
                    }
                 
            }
    }