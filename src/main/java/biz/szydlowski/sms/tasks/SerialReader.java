/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.tasks;

import biz.szydlowski.sms.ATCommandsManager;
import static biz.szydlowski.sms.JDaemonSms.functions;
import biz.szydlowski.sms.Messaging;
import static biz.szydlowski.sms.WorkingObjects.ioErrorDetected;
import static biz.szydlowski.sms.WorkingObjects.lastSerialRead;
import biz.szydlowski.sms.WorkingStats;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Dominik
 */
public class SerialReader implements Runnable {
    
            static final Logger logger =  LogManager.getLogger( SerialReader.class);
            InputStream in;            

            public SerialReader( InputStream in ) {
              this.in = in;
            }

            @Override
            public void run() {
              byte[] buffer = new byte[64]; //wiekszy buffor
              int len = -1;
              try {
                String ret = "";
                StringBuilder sb = new StringBuilder();
                while( ( len = this.in.read( buffer ) ) > -1 ) {
                   ret = new String( buffer, 0, len );
                   if (ret.replaceAll("\\s+", "").replaceAll("\r", "").length()>0){
                        lastSerialRead = System.currentTimeMillis();
                        /*if (ATCommandsManager.isWriteLockedStatus()){
                           logger.debug("WAIT Command is in write mode......");
                        }*/
                        sb.append(ret.replaceAll("\r", ""));
                        if (sb.toString().contains("+CMTI:")){
                            logger.debug("DETECTED new incoming SMS");
                            if (!ATCommandsManager.isWriteLockedStatus()){
                                //można wyczyscic bufor jeśli nie było OK i ERROR
                                 if (!sb.toString().contains("ERROR") && !sb.toString().contains("OK")){ 
                                     logger.debug("Clear buffor...");
                                     ret = "";
                                     sb.delete(0, sb.length());
                                 }
                            }
                            new HotSmsRead().start();
                        } else  if (sb.toString().contains("+CLIP") && sb.toString().contains("RING") && sb.toString().split(",").length>=6){
                            logger.debug("DETECTED new calling");
                            Messaging.addHangupCalltoQueue();
                            WorkingStats.AtSendCommandsPlus();
                          
                            if (!sb.toString().contains("ERROR") && !sb.toString().contains("OK")){ 
                                
                                String call []= sb.toString().split(",");
                                 if (call[0]!=null){
                                     call[0] = call[0].replaceAll("\\+CLIP:", "");
                                     call[0] = call[0].replaceAll("\"", "");
                                     call[0] = call[0].replaceAll("RING", "");
                                     call[0] = call[0].replaceAll("\n", "");
                                     call[0] = call[0].replaceAll("\\s+", "");
                                     logger.debug("Call " + call[0]);
                                     if (functions.isValidUserAPIForPhone(call[0])){
                                        Messaging.addSMStoQueue(call[0], "HEARTBEAT. I'M ALIVE!!!", false);
                                     }
                                 }
                                logger.debug("Clear buffor...");
                                ret = "";
                                sb.delete(0, sb.length()); 
                            }
                           
                           
                        }
                  } 
                  //remove else (BUG-20170320) ++++++
                  if (ATCommandsManager.isWriteLockedStatus()){
                       UUID cUUID = ATCommandsManager.getCurrentUUID();
                       if (cUUID!=null){
                           if (sb.toString().contains("OK")){
                                WorkingStats.OkATCountPlus(); 
                                String sbString=sb.toString(); 
                                logger.debug("Clear buffor...");
                                sb.delete(0, sb.length());
                                ret = "";
                              
                                logger.debug("AT RETURN: " + sbString + " for " + cUUID);
                                if (Messaging.delete_after_done.getOrDefault(cUUID, true).toString().equals("true")){
                                    Messaging.deleteFromMessaging(cUUID, true, 0);
                                    ATCommandsManager.unlockWrite();
                                } else {
                                    Messaging.at_return.put(cUUID, sbString);
                                    Messaging.at_status.put(cUUID, "OK");
                                    Messaging.at_done.put(cUUID, true);
                                    Messaging.queue_uuid.remove(cUUID);
                                    ATCommandsManager.unlockWrite();

                                } 
                             

                            } else if (sb.toString().contains("ERROR")){
                                
                                WorkingStats.ErrorATCountPlus();
                                String sbString=sb.toString(); 
                                logger.debug("Clear buffor...");
                                sb.delete(0, sb.length());
                                ret = "";
                                
                                logger.debug("(ERR) AT RETURN: " + sbString + " for " + cUUID);

                                Messaging.at_return.put(cUUID, sbString);
                                Messaging.at_status.put(cUUID, "ERROR");
                                Messaging.at_done.put(cUUID, true);

                                //BUG 20170516
                                if ( Messaging.retry_type.getOrDefault(cUUID, false)){
                                     //zrobi at_done=false
                                     Messaging.reanimate(cUUID); //lock write// unlock

                                } else { 
                                    
                                     
                                    if (Messaging.delete_after_done.getOrDefault(cUUID, true).toString().equals("true")){
                                        Messaging.deleteFromMessaging(cUUID, false, 1); //dead
                                        ATCommandsManager.unlockWrite();
                                    } else {
                                        Messaging.at_return.put(cUUID, sbString);
                                        Messaging.at_status.put(cUUID, "ERROR");
                                        Messaging.at_done.put(cUUID, true);
                                        //Messaging.queue_uuid.remove(cUUID);
                                        ATCommandsManager.unlockWrite();

                                    } 

                                }

                            } 
                       }
                   } 
                } 
                
                 
                
              } catch( IOException e ) {
                  ioErrorDetected = true;
                  logger.error("E002 " + e);
              }
            }
    }