/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.tasks;

import biz.szydlowski.sms.ATCommandsManager;
import biz.szydlowski.sms.Messaging;
import static biz.szydlowski.sms.WorkingObjects.error_sms_expire;
import static biz.szydlowski.sms.WorkingObjects.ioErrorDetected;
import static biz.szydlowski.sms.WorkingObjects.lastSerialWrite;
import biz.szydlowski.sms.WorkingStats;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Dominik
 */
public class SerialWriter implements Runnable {
           
            static final Logger logger =  LogManager.getLogger( SerialWriter.class);
            
            OutputStream out;
            boolean write=true;

            public SerialWriter( OutputStream out ) {
              this.out = out;
            }

            @Override
            public void run() {
              try {
              //  int c = 0;
                while(write) {
                     
                    //if (Messaging.at_done.size()>20) logger.debug("Total messaging: " + Messaging.at_done.size());
                  
                 if (!Messaging.queue_uuid.isEmpty()){
                   
                     if (!ATCommandsManager.isWriteLockedOrTimeout()){
                          UUID uuid = (UUID) Messaging.queue_uuid.peek();
                       
                          if (ATCommandsManager.wasTimeout()){
                               //queue_uuid_main.poll();
                               if (!Messaging.at_msg_to_send.getOrDefault(uuid, "df").contains("ATE0"))
                                  Messaging.reanimate(uuid);
                               continue;                            
                          }

                          boolean isDone=false;

                          if (Messaging.at_done.get(uuid)!=null){
                              isDone=Boolean.parseBoolean(Messaging.at_done.get(uuid).toString());
                          } else {
                               logger.warn("UUID " + uuid + " not found!!!");
                               Messaging.queue_uuid.poll();
                               continue;
                          }

                          if ( isDone){
                              logger.debug("(DONE) UUID " + uuid);
                              Messaging.at_done.put(uuid, true);
                              Messaging.queue_uuid.poll();
                              continue;
                          }
                          String atcmd = "AT";


                          if (Messaging.at_msg_to_send.get(uuid)!=null){
                               atcmd=Messaging.at_msg_to_send.get(uuid);
                          } 
                          
                          Messaging.at_status.put(uuid, "doing");

                          if (!error_sms_expire || !atcmd.startsWith("AT+CMGS")){
                              this.out.write(atcmd.getBytes());
                              WorkingStats.AtSendCommandsPlus();
                          } else {
                              this.out.write("AT\n".getBytes());
                              WorkingStats.AtSendCommandsPlus();
                          }
                          
                          ATCommandsManager.lockWrite();
                          ATCommandsManager.setCurrentUUID(uuid);

                          lastSerialWrite = System.currentTimeMillis();

                          logger.debug("SET CURRENT UUID " + uuid);
                          logger.debug("Execute from messaging " + atcmd);

                          //usuniecie ++20170323
                         // UUID del= Messaging.queue_uuid.poll();
                         // logger.debug("del " + del);
                          //Messaging.at_done.put(uuid, true);

                          logger.debug("Wait for response....");
                     }  
                     
                      try {
                            Thread.sleep(250);
                      }
                      catch (InterruptedException e)    {
                          Thread.currentThread().interrupt(); // restore interrupted status
                      }

                      
                } // no empty  

                
                 try {
                        Thread.sleep(100);
                 }
                 catch (InterruptedException e)    {
                       Thread.currentThread().interrupt(); // restore interrupted status
                 }
                 
              
                }
              } catch( IOException e ) {
                  ioErrorDetected = true;
                  logger.error("E003 " + e);
              }
            }
    }
    