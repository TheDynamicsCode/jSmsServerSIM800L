/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms;


import static biz.szydlowski.sms.WorkingObjects.RETRYCOUNT;
import static biz.szydlowski.sms.WorkingObjects.archiveToFile;
import static biz.szydlowski.sms.WorkingObjects.error_sms_expire;
import static biz.szydlowski.sms.WorkingObjects.serverApi;
import biz.szydlowski.utils.OSValidator;
import static biz.szydlowski.utils.tasks.TasksWorkspace.absolutePath;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author dominik
 */
public class Messaging {
  
    static final Logger logger =  LogManager.getLogger(Messaging.class);
    public static List<Long> sms_sender = null;
    public static Map<UUID, String> at_msg_to_send ;
    public static Map<UUID, Boolean> retry_type ;
    public static Map<UUID,Integer> retry_int;
    public static Map<UUID, String> at_return;
    public static Map<UUID, Boolean> delete_after_done;
    public static Map<UUID, Boolean> at_done;
    public static Map<UUID, String> at_status; 
    public static List<UUID> hidden_uuid = new ArrayList<>();
    public static Queue<UUID> queue_uuid = new ArrayDeque<>();
    public static Queue<UUID> queue_locked = new ArrayDeque<>();
    private static List<String> last_outbound_sms = null; 
    private static List<String> last_inbound_sms = null;
    public static UUID last_uuid;
    public static boolean lockWhenDumpLoad=false;
    public static boolean lockSemafor=false;
    private static final String CRZ = new String(new char[] { 26 }); 
   // private static String CRZ = "\n";
    private static final Thread lockThread=null;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss"); 
    private static final SimpleDateFormat timestamp = new SimpleDateFormat("** yyyy-MM-dd ** HH:mm:ss **"); 
    //private static String CRZ = new String(new char[] { '\032' });

    static   {
        at_msg_to_send = new HashMap<>();
        retry_type = new HashMap<>();
        retry_int= new HashMap<>();
        at_return = new HashMap<>(); 
        delete_after_done = new HashMap<>();
        at_done= new HashMap<>(); 
        at_status = new HashMap<>();
        last_outbound_sms = new ArrayList<>(); 
        last_inbound_sms = new ArrayList<>(); 
        sms_sender = new ArrayList<>(); 
    }  
    
    public static int getQueueSize(){
           return queue_uuid.size();
    } 
    
    public static Queue<UUID> getQueueUUID(){
        return queue_uuid;
    }
    
    public static String getAtMsgToSend(UUID uuid){
        if (Messaging.at_done.get(uuid)==null){
             return "NULL"; //brak w msg czyli done!
         } else  {
             return Messaging.at_msg_to_send.get(uuid);
         }
    }
     
    public static UUID addtoMessaging(String at_msg, boolean isRetry, boolean deleteAfterDone){
        if (lockSemafor){
            logger.info("Lock semafor enabled.. wait..");
        }
        while (lockSemafor){
           try {
                lockThread.sleep(100);
            }  catch (InterruptedException e)    {
                lockThread.currentThread().interrupt(); // restore interrupted status
           }
        }
        lockSemafor=true;
        last_uuid = UUID.randomUUID();
        retry_type.put(last_uuid, isRetry); 
        retry_int.put(last_uuid, 0);
        at_msg_to_send.put(last_uuid, at_msg);
        at_return.put(last_uuid, "nothing");
        at_done.put(last_uuid, false);
        at_status.put(last_uuid, "init"); 
        delete_after_done.put(last_uuid, deleteAfterDone);
        queue_uuid.add(last_uuid);
        lockSemafor=false;
        return last_uuid;
    }
    
    public static void reanimate(UUID uuid){
        logger.info("*** UUID reanimate " + uuid + " *****");
        ATCommandsManager.lockWrite();
                
        queue_uuid.remove(uuid);
        queue_uuid.add(uuid);
       
        if (Messaging.retry_type.get(uuid).toString().equals("true")){
          
            int retrycount=0;
            try {
                 retrycount = Integer.parseInt(Messaging.retry_int.get(uuid).toString());
           } catch (Exception ee){}

            if (retrycount<RETRYCOUNT){
                retrycount++;  
                logger.debug("Retry for " + uuid); 
                at_done.put(uuid, false);
                retry_int.put(uuid, retrycount);
            } else {
                    logger.debug("Retry >= " + RETRYCOUNT + " for " + uuid);
                    logger.debug("Delete from messaging " + uuid);
                    //bug 20180221
                    Messaging.deleteFromMessaging(uuid, false, 2);
            }

        } else {
            Messaging.deleteFromMessaging(uuid, false, 1);  
        
        }
        ATCommandsManager.unlockWrite();
    }

   
    public static boolean isDone(UUID key){
         if (Messaging.at_done.get(key)==null){
             return true; //brak w msg czyli done!
         } else   if ( Messaging.at_done.get(key).toString().equalsIgnoreCase("true")) return true;
         else return false;
    } 
    
    private static int isHidden(UUID key){
        int index = -1;
        for (int i=0; i<hidden_uuid.size(); i++){
            if (hidden_uuid.get(i).equals(key)){
                index = i;
                break;
            }
        }
        
        return index;
    }
    
    public static void deleteFromMessaging(UUID key, boolean done_without_errors, int codeError){   
        
        logger.debug("Delete from Messaging " + key);
        
        if (lockWhenDumpLoad){
            queue_locked.add(key);
        } else {
           
            if (!done_without_errors){
                dumpToFile(key, codeError); //dump to dead
            }
        
            
            String msg = at_msg_to_send.get(key);
            if (msg!=null){
                if (msg.contains("AT+CMGS")){
                                 
                   String phone_number = "default";
                   int m_start = msg.indexOf("\"")+15;
                   
                   if (m_start>msg.length()) m_start=0;
                   if (m_start<0) m_start=0;
                   
                   try {
                     phone_number= msg.substring(msg.indexOf("\"")+1, msg.indexOf("\"")+13);
                   } catch (Exception e){
                       
                   }
                    while (last_outbound_sms.size()>serverApi.getMaxOutboundMsgWeb()){
                        //usuwanie dziadostwa
                        last_outbound_sms.remove(0);
                    }
                    
                    if (msg.contains("CURRENT TOKEN")){
                         msg = "CURRENT TOKEN: XXXXXXX (valid 10 minutes)";
                    }
                                 
                    if (done_without_errors){
                        if (error_sms_expire){
                             dumpToFile(key,  3); //dump to expired
                        }
                        //wyslane skutecznie!!!!
                        int indx =isHidden(key); 
                        if (isHidden(key)>-1){
                           WorkingStats.smsSendCountPlus(); 
                           archiveToFile.addToRepository("sms_outbound_txt.out", phone_number+"\t The message is hidden, length " + msg.length());
                           last_outbound_sms.add(new StringBuilder().append(sdf.format(new Date())).append("\t The message is hidden, length ").append(msg.length()).toString());
                           hidden_uuid.remove(indx);
                        } else {
                           archiveToFile.addToRepository("sms_outbound_txt.out", phone_number+"\t" + msg.substring(m_start).replaceAll("\n", " ").replaceAll("\r", ""));
                           WorkingStats.smsSendCountPlus();
                           last_outbound_sms.add(new StringBuilder().append(sdf.format(new Date())).append("\t").append(msg).toString());
                        }
                       
                        sms_sender.add(System.currentTimeMillis());
                        
                 
                    } else {
                        int indx =isHidden(key); 
                        if (isHidden(key)>-1){
                           archiveToFile.addToRepository("sms_error_outbound_txt.out", phone_number+"\t The message is hidden, length " + msg.length());
                            hidden_uuid.remove(indx);
                        } else {
                           archiveToFile.addToRepository("sms_error_outbound_txt.out", phone_number+"\t" + msg.substring(m_start).replaceAll("\n", " ").replaceAll("\r", ""));
                        }
                    }
                }
            }
            
            retry_type.remove(key);
            retry_int.remove(key);
            at_msg_to_send.remove(key);
            at_return.remove(key);
            at_done.remove(key);
            at_status.remove(key);
            delete_after_done.remove(key);
            queue_uuid.remove(key);
           
        }
     }  
    
    
    public static void clearMessaging(){       
        retry_type.clear();
        retry_int.clear();
        at_msg_to_send.clear();
        at_return.clear();
        at_done.clear();
        at_status.clear();
        delete_after_done.clear();
        queue_uuid.clear();
    } 
    
    
    public static UUID addSMStoQueue(String phone, String msg, boolean isHidden){
        
        UUID hand_uuid=null;
       
                
        if ( sms_sender.size()>=serverApi.getMaxSmsInHour()) {
            logger.warn("SMS per hour LIMIT!!!!!!!");
           
            if (isHidden){
                archiveToFile.addToRepository("sms_error_outbound_txt.out", phone+"\t The message is hidden, length " + msg.length());
                      
            } else {
                archiveToFile.addToRepository("sms_error_outbound_txt.out", phone+"\t" + msg);
            }
            
        } else {
                    
            hand_uuid = addtoMessaging("AT+CMGS=\""+phone+"\"\n"+msg+"\n"+timestamp.format(new Date())+"\n" +CRZ, true, true); //retry  
            if (isHidden){            
                hidden_uuid.add(hand_uuid);
            }  
          
        }
        
       
        
        return hand_uuid;
    }  
    
    public static UUID addHangupCalltoQueue(){
        UUID hand_uuid = addtoMessaging("AT+CHUP\n", false, true);      
        return hand_uuid;
    }
   
   public static List<String> getLastOutboundSms() {
      return last_outbound_sms;
   } 
   
   public static void addToLastInboundSms(String sms){
       while (last_inbound_sms.size()>serverApi.getMaxInboundMsgWeb()){
            //usuwanie dziadostwa
            last_inbound_sms.remove(0);
        }

        last_inbound_sms.add(sms);
       
   }
   
   public static List<String> getLastInboundSms() {
      return last_inbound_sms;
   }
    
 
    public static void loadFromFiles(){ 
        String _messaging="messaging";
        
        if (OSValidator.isUnix()){
              _messaging = absolutePath + "/" +  _messaging;
        }  
        
        File f = new File(_messaging);

        if (!f.exists()) {
            f.mkdirs();
        }   
        
        File[] msgFolder = f.listFiles();
        for (File msgF : msgFolder) {
                     
            if (msgF.isFile()) {
                if (getExtension(msgF.getName()).equalsIgnoreCase("msg") ){
                    try {
                        logger.debug("Read msg file " + msgF.getName());
                       
                        if (msgF.getName().length()<4){
                            logger.error("(R) Skipping file " + msgF + ". Incorrect filename!!!!!") ;
                            continue;
                        }
                        
                        String uuid = msgF.getName().substring(0, msgF.getName().length()-4);
                                             
                        Properties props = loadProps(msgF.getCanonicalPath());
                        UUID readUuid = UUID.fromString(props.getProperty("uuid", "01f3cf39-fb6e-11e6-874a-4c72b97ca1e7")); 
                        
                        if (!uuid.equals(props.getProperty("uuid", "01f3cf39-fb6e-11e6-874a-4c72b97ca1e7"))){
                            logger.error("Skipping file " + readUuid + " " + uuid) ;
                            logger.error("Skipping file " + msgF + ". Incorrect filename!!!!!") ;
                            msgF.renameTo(new File(msgF+"#"));
                            continue;
                        }
                        
                        retry_type.put(readUuid, Boolean.parseBoolean(props.getProperty("retry_type", "true")));
                        //retry_int.put(readUuid, Integer.parseInt(props.getProperty("retry_int", "1")));
                        if ( Integer.parseInt(props.getProperty("retry_int", "1")) >= RETRYCOUNT){
                           logger.info("correct retry to 0");
                            retry_int.put(readUuid, 0);
                        } else {
                            retry_int.put(readUuid, Integer.parseInt(props.getProperty("retry_int", "1")));
                        }
                        at_return.put(readUuid, props.getProperty("at_return", "wait"));
                        at_done.put(readUuid, Boolean.parseBoolean(props.getProperty("at_done", "false")));
                        at_status.put(readUuid, props.getProperty("at_status", "wait"));
                        delete_after_done.put(readUuid, Boolean.parseBoolean(props.getProperty("delete_after_done", "true")));
                        at_msg_to_send.put(readUuid, props.getProperty("at_msg_to_send", "AT"));
                        queue_uuid.add(readUuid);
                        last_uuid = readUuid;
                        
                    } catch (IOException ex) {
                        logger.error("Error 005" + ex);
                    }  
                    
                    if (msgF.delete()){
                
                    }
              
                }
              
            }
                
                
        }
         
        
       
    }
    
    public static Properties loadProps(String filename){
        
        InputStream input = null;
	Properties prop = new Properties();

	try {

		input = new FileInputStream(filename);
		// load a properties file
		prop.load(input);


	} catch (IOException ex) {
		logger.error(ex);
	} finally {
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}
        
        return prop;

    }
   
    
    public static void dumpToFiles(){
        lockWhenDumpLoad=true; 
        
        Iterator uuids  = queue_uuid.iterator();
        while(uuids.hasNext()) {
            dumpToFile( (UUID) uuids.next(), 0);
        }
        
        lockWhenDumpLoad=false;
        while (!queue_locked.isEmpty()){
             logger.info("Delete locked dump/load msg " + queue_uuid.peek());
             deleteFromMessaging(queue_locked.poll(), true, 0); // nie dumpuje już
        }
         
        
    }  
    
    private static void dumpToFile(UUID key, int type){ 
     
        
        String _dumpfile="messaging/"+key+".msg";
        String _messaging="messaging";
        
        switch (type) {
            case 1:
                _dumpfile="messaging_deadqueue/"+key+".msg";
                _messaging="messaging_deadqueue";
                break;
            case 2:
                _dumpfile="messaging_retryerror/"+key+".msg";
                _messaging="messaging_retryerror";
                break;
            case 3: 
                _dumpfile="messaging_sms_expired/"+key+".msg";
                _messaging="messaging_sms_expired";
                break;
            default:
                break;
        }
        
        if (OSValidator.isUnix()){
              _dumpfile = absolutePath + "/" + _dumpfile;
              _messaging = absolutePath + "/" +  _messaging;
        }  
        
        File f = new File(_messaging);

        if (!f.exists()) {
            f.mkdirs();
        }   
        
        Properties props = new Properties();
        props.put("uuid", key.toString());
        
        String value = "";
        if (retry_type.get(key)!=null){
            value = retry_type.get(key).toString();
        } else {
            return;
        }
        
        props.put("retry_type", value);
        
        
        if (retry_int.get(key)!=null){
            value = retry_int.get(key).toString();
        } else {
            return;
        }
        props.put("retry_int", value); 
    
        
        if (at_msg_to_send.get(key)!=null){
            value = at_msg_to_send.get(key);
        } else {
            return;
        }
        props.put("at_msg_to_send", value); 
        
        
        if (at_return.get(key)!=null){
            value = at_return.get(key);
        } else {
            return;
        }
        props.put("at_return", value);   
       
        
        if (at_done.get(key)!=null){
            value = at_done.get(key).toString();
        } else {
            return;
        }
        props.put("at_done", value);  
        
      
        if (at_status.get(key)!=null){
            value = at_status.get(key);
        } else {
            return;
        }
        props.put("at_status", value);   
        
        if (delete_after_done.get(key)!=null){
            value = delete_after_done.get(key).toString();
        } else {
            return;
        }
        props.put("delete_after_done",  value);  
        
        
        OutputStream output = null;
  	
        try {

		output = new FileOutputStream( _dumpfile);
		props.store(output, null);

	} catch (IOException io) {
		logger.error(io);
	} finally {
		if (output != null) {
			try {
				output.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}

	}
    }
    
     private  static String getExtension(String filename) {
          if (filename == null) return null;
          final String afterLastSlash = filename.substring(filename.lastIndexOf('/') + 1);
          final int afterLastBackslash = afterLastSlash.lastIndexOf('\\') + 1;
          final int dotIndex = afterLastSlash.indexOf('.', afterLastBackslash);
          return (dotIndex == -1) ? "" : afterLastSlash.substring(dotIndex + 1); 
     }
       
      
                
                
      
}
