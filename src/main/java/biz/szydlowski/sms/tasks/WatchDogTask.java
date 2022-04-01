/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.tasks;


import static biz.szydlowski.sms.JDaemonSms.functions;
import static biz.szydlowski.sms.JDaemonSms.serialPort;
import static biz.szydlowski.sms.JDaemonSms.statistics;
import static biz.szydlowski.sms.WorkingObjects.*;
import biz.szydlowski.sms.WorkingStats;
import java.text.SimpleDateFormat;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Dominik
 */
public class WatchDogTask extends TimerTask {
    
           static final Logger logger =  LogManager.getLogger(WatchDogTask.class);
           private SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss"); 
           private SimpleDateFormat sdf_dis = new SimpleDateFormat("ddMMyyyyHHmm"); 
        
            @Override
            public void run() {
                
                try {
                
                    if (restart_watchdog){
                        restart_watchdog=false;
                        functions.restart();
                    }

                    if (kill_watchdog){
                         functions.disasterDump("kill");
                         System.exit(-100);
                    }


                    if (System.currentTimeMillis()-lastSMSReader > smsreadertime_sec*1200){
                          logger.fatal("Detected IDLE SMS READER");
                          statistics.disasterSmsReadCountPlus();
                          functions.disasterDump("smsr");
                          functions.restart();
                    }

                    if (System.currentTimeMillis()-lastMaintenance > 300000){ //5minut
                          logger.fatal("Detected IDLE Maintenance");
                          statistics.disasterMaintenanceCountPlus();
                          functions.disasterDump("mt");
                          functions.restart();
                    } 

                    if (System.currentTimeMillis()-lastUserSession> 300000){ //5minut
                           logger.fatal("Detected IDLE UserSession");
                           statistics.disasterUserSessionCountPlus();
                           functions.disasterDump("us");
                           functions.restart();
                    } 

                    if (System.currentTimeMillis()-lastSerialRead> smsreadertime_sec*1200){ //5minut
                          logger.fatal("Detected IDLE SerialRead");
                          statistics.disasterSerialReadCountPlus();
                          functions.disasterDump("sr");
                          functions.restart();
                    } 

                    if (System.currentTimeMillis()-lastSerialWrite> smsreadertime_sec*1200){ //5minut
                          logger.fatal("Detected IDLE SerialWrite");
                          statistics.disasterSerialWriteCountPlus();
                          functions.disasterDump("sw");
                          functions.restart();
                    }

                    if (serialPort==null){
                         logger.fatal("serialPort==null, retarting system....");
                         statistics.disasterSerialPortNullCountPlus();
                         functions.disasterDump("sp");
                         functions.restart();
                    } 

                    if (ioErrorDetected){
                        logger.fatal("ioErrorDetected, retarting system....");
                        statistics.disasterIOErrorCountPlus();
                         functions.disasterDump("io");
                         functions.restart();
                   }

                    if (WorkingStats.getOkATCount() - lastOK > 0 ){
                        atErrorThreshold = 0;
                    }

                    if (WorkingStats.getErrorATCount() - lastErrors > 0 ){
                        atErrorThreshold ++;
                    }

                    if (atErrorThreshold  > serverApi.getAtErrorThreshold()){ 
                          logger.fatal("Detected atErrorTH ");
                          statistics.disasterSmsReadCountPlus();
                          functions.disasterDump("atErTH");
                          functions.restart();
                    }

                    lastErrors = WorkingStats.getErrorATCount();
                    lastOK = WorkingStats.getOkATCount();
                } catch (Exception e){
                    logger.fatal(e);
                }
                  
            }    
        
         
       
    }  
