/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms;

import biz.szydlowski.sms.Quartz.CronQuartz;
import static biz.szydlowski.sms.WorkingObjects.smsCommands;
import static biz.szydlowski.sms.WorkingObjects.usersApi;
import biz.szydlowski.sms.web.SmsWebServer;
import biz.szydlowski.sms.utils.Statistics;
import biz.szydlowski.sms.tasks.WatchDogTask;
import biz.szydlowski.utils.Constans;
import biz.szydlowski.utils.Memory;
import biz.szydlowski.utils.OSValidator;
import java.io.OutputStream;
import gnu.io.CommPortIdentifier; 
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Timer;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import static biz.szydlowski.sms.WorkingObjects.*;
import biz.szydlowski.sms.tasks.MaintenanceTask;
import biz.szydlowski.sms.tasks.SerialReader;
import biz.szydlowski.sms.tasks.SerialWriter;
import biz.szydlowski.sms.tasks.SmsReadTask;
import biz.szydlowski.sms.tasks.TokenTask;
import biz.szydlowski.sms.tasks.UserSessionTask;
import biz.szydlowski.utils.tasks.TasksWorkspace;
import static biz.szydlowski.utils.tasks.TasksWorkspace.absolutePath;
import java.io.File;
import java.net.URLDecoder;
import java.security.CodeSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class JDaemonSms implements Daemon {
    
       
        static {
            try {
                System.setProperty("log4j.configurationFile", getJarContainingFolder(JDaemonSms.class)+"/setting/log4j/log4j2.xml");
            } catch (Exception ex) {
            }
        }
    
	private static boolean TESTING = false;
        private static boolean stop = false;
        static final Logger logger =  LogManager.getLogger("biz.szydlowski.sms.JDaemonSms");
       
        private static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss"); 
       // public static String logFilePath="log";    
        
        private Timer WatchDog = null;
        private Timer SMSReaderTimer = null; 
        private Timer Maintenance = null;
        private Timer UserSessionWorker = null;
        private Timer TokenWorker = null;
        public static Functions functions = null;
    
        public static Statistics statistics  = null;
    
        public static SerialPort serialPort=null;               
          
     
        private SmsWebServer smsWebServer = null;
                      
        
        private CronQuartz _CronQuartz = null;
      
        
     
     public JDaemonSms(){
        	
     } 
    
     public JDaemonSms(boolean test, boolean win){
          if (TESTING || test || win){
            if (!win) System.out.println("****** TESTING MODE  ********"); 
            else System.out.println("****** WINDOWS MODE  ********"); 
            try {
               if (initialize()){
                    start();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ex);
            }
        }
     }
    
        
            
     public static void main(String[] args) {
         if (TESTING){
             JDaemonSms jDaemonSms = new JDaemonSms(true, false);
         }  else {
             if (args.length>0){
                 if (args[0].equalsIgnoreCase("testing")){
                     JDaemonSms jDaemonSms = new JDaemonSms(true, false);
                 }
                 
             }
         } 
		
     }
   
     
     public boolean initialize() {
                
      
            if (OSValidator.isUnix()){
                 absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                       absolutePath = "/" + absolutePath.substring(0, absolutePath.lastIndexOf("/"))+"/";
            }
                   
            functions = new Functions();             
            
            statistics = new Statistics();
                        
            WorkingObjects.init();           
                       
            WatchDog = new Timer("WatchDog", true);
            SMSReaderTimer = new Timer("SMSReaderTimer", true); 
            Maintenance = new Timer("Maintenance", true);
            UserSessionWorker = new Timer("UserSessionWorker", true);
            TokenWorker = new Timer("TokenWorker", true);
            
                   
            logger.info(Constans.STARTER_JSMS);
            logger.info(new Version().getAllInfo()); 
           
            logger.info("java.library.path " + System.getProperty("java.library.path"));
                 
            TasksWorkspace.start(Version.DEV_VERSION_EXPIRE_DATE, false);        
            WorkingStats.start();
          
            statistics.loadStats();  
            statistics.runsPlus();
           
          
            archiveToFile.addToRepository("runs.out", "START\t" + sdf.format(new Date()));  
           
                      
            WatchDog.schedule(new WatchDogTask(), 10000, 5000); 
            SMSReaderTimer.schedule(new SmsReadTask(), 60000, smsreadertime_sec*1000); 
            Maintenance.schedule(new MaintenanceTask(), 60000, 30000);
            UserSessionWorker.schedule(new UserSessionTask(), 60000, 60000);
            TokenWorker.schedule(new TokenTask(), 0, 300000); //5 minut
                
            CommPortIdentifier portId = null;
            Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

            //First, Find an instance of serial port as set in PORT_NAMES.
            while (portEnum.hasMoreElements()) {                  
                    CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
                    logger.debug("Detected port " + currPortId.getName());
                    if (currPortId.getName().equals(modemApi.getPortNameMaster()) ||
                            currPortId.getName().equals(modemApi.getPortNameFirstSlave()) ||
                            currPortId.getName().equals(modemApi.getPortNameSecondSlave() )) {
                            portId = currPortId;
                            break;
                    }
                    
            }
            if (portId == null) {
                 logger.error("Could not find COM ports: " + modemApi.getPortNameMaster()+", "
                 + modemApi.getPortNameFirstSlave()+", "
                 + modemApi.getPortNameSecondSlave());
                 return false;
            } 

            try {
                                 
                    // open serial port, and use class name for the appName.
                                 
                    serialPort = (SerialPort) portId.open(this.getClass().getName(), modemApi.getPortTimeout());
                    
                    // set port parameters
                    int PORT_DATA_RATE = modemApi.getPortDataRate();
                    serialPort.setSerialPortParams(PORT_DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                   
                    InputStream in = serialPort.getInputStream();
                    OutputStream out = serialPort.getOutputStream();
                                      
                    Messaging.addtoMessaging("ATE0\n", false, true);      
                    Messaging.addtoMessaging("AT+CGMI\n", false, true);   
                    Messaging.addtoMessaging("AT+CLIP=1\n", false, true);   
                    Messaging.addtoMessaging("AT+CMGF=1\n", false, true);
                    Messaging.addtoMessaging("AT+CSCS=\"IRA\"\n", false, true);
                    Messaging.addtoMessaging("AT+CSCA=\""+modemApi.getAT_CSCA()+"\"\n", false, true);
                   
                    usersApi.stream().filter((user) -> (user.isActiveAtStartup() && user.isAdmin())).map((user) -> user.getPhone()).forEachOrdered((phone) -> {
                        Messaging.addSMStoQueue(phone, "SMS SERVER WAS STARTED...ENJOY!!!", false);
                    });
                          
        
                    
                    for (String s :  _server.getStartingCommand()) { 
                        Messaging.addtoMessaging(s+"\n", false, true);                      
                    } 
                    
                    functions.disasterRecovery();
                    
                    ( new Thread( new SerialReader( in ), "SerialReader" ) ).start();
                    ( new Thread( new SerialWriter( out ), "SerialWriter" ) ).start();
                      
                    smsCommands.add("help"); 
                    smsCommands.add("heartbeat");
                    smsCommands.add("login"); 
                    smsCommands.add("login.<time>"); 
                    smsCommands.add("logout");
                    smsCommands.add("MapCommands");
                    smsCommands.add("Version");
                    smsCommands.add("Uptime"); 
                    smsCommands.add("Stats");
                    smsCommands.add("FullStats");
                    smsCommands.add("StartTime");
                    smsCommands.add("SmsExpireDate");
                    smsCommands.add("admin.getActiveUsers");
                    smsCommands.add("admin.getDefinedUsers");                 
                    smsCommands.add("admin.login.user.<user>");
                    smsCommands.add("admin.logout.user.<user>");
                    smsCommands.add("admin.restart.server");
                    smsCommands.add("admin.execute.<cmd>");
                    smsCommands.add("admin.at.command.<atcommand>");
                    smsCommands.add("admin.at.restart");
                    smsCommands.add("admin.kill.self");
                    smsCommands.add("You can input multiple commands using a split char \";\".");
                    smsCommands.add("All commands ignore case sensitive.");
               
                    smsWebServer = new SmsWebServer(serverApi.getWebPort());
                    if (serverApi.getQuartzEnabled()) _CronQuartz = new  CronQuartz ();
                    
                    logger.info("initialize done");
                    
                    Memory.start();
                     
                    return true;
        
            } catch (PortInUseException | NumberFormatException | UnsupportedCommOperationException | IOException e) {
                    logger.error("E001 " +e.toString());
                    return false;
            }
	}

    /**
     * This should be called when you stop using the port.
     * This will prevent port locking on platforms like Linux.
     */
    public synchronized void closePort() {
            if (serialPort != null) {
                    serialPort.removeEventListener();
                    serialPort.close();
            }
    }

    /**
     *
     * @param dc
     * @throws DaemonInitException
     * @throws Exception
     */
    @Override
    public void init(DaemonContext dc) throws DaemonInitException, Exception {
          //String[] args = dc.getArguments();
          initialize();
         
    }

  
    @Override
    public void start() throws Exception {
          logger.info("Starting server");
          ioErrorDetected=false;
          smsWebServer.start();   
          if (serverApi.getQuartzEnabled()) _CronQuartz.doJob();
          logger.info("Started server");
    }

   
    @Override
    public void stop() throws Exception {
        logger.info("Stopping daemon");
        
        functions.stopDump();
        
        smsWebServer.stopSever();
        if (serverApi.getQuartzEnabled()) _CronQuartz.stop();
        
        int active = Thread.activeCount();
        Thread all[] = new Thread[active];
        Thread.enumerate(all);

        for (int i = 0; i < active; i++) {
            if (all[i]!=null){
                if (!all[i].getName().contains("RESTART")){
                   logger.info("Thread to interrupt " + i + ": " + all[i].getName() + " " + all[i].getState());
                   all[i].interrupt();
                } else {
                    logger.info("Thread alive " + i + ": " + all[i].getName() + " " + all[i].getState());
                }
            }
        }
       
        WorkingObjects.clear();
                    
        WatchDog.cancel();
        SMSReaderTimer.cancel();
        Maintenance.cancel();
        UserSessionWorker.cancel();
        TokenWorker.cancel();
     
        closePort();
        
        logger.info("Stopped daemon");
    }
    
    //for windows
    public static void start(String[] args) {
       System.out.println("start");
       JDaemonSms jDaemonSms = new JDaemonSms(false, true);
        
        while (!stop) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }
  
    public static void stop(String[] args) {
        System.out.println("stop");
        stop = true;
       
        logger.info("Stoppping daemon");
            
        functions.stopDump();
           
        int active = Thread.activeCount();
        Thread all[] = new Thread[active];
        Thread.enumerate(all);

        for (int i = 0; i < active; i++) {
            if (!all[i].getName().contains("RESTART")){
               logger.info("Thread to interrupt " + i + ": " + all[i].getName() + " " + all[i].getState());
               all[i].interrupt();
            } else {
                logger.info("Thread alive " + i + ": " + all[i].getName() + " " + all[i].getState());
            }
        }
        
        WorkingObjects.clear();
    
        logger.info("Stopped daemon");  
        
        System.exit(0);
                
    }
 

   
    @Override
    public void destroy() { 
        logger.info("Destroy daemon");
        
        WatchDog = null;
        SMSReaderTimer = null; 
        Maintenance = null;
        UserSessionWorker = null;
        TokenWorker = null;
    
        statistics  = null;    
        
        serialPort=null;       
      
        archiveToFile = null;
        smsWebServer = null;       
        
     
        logger.info("*********** Destroyed daemon  ****************");
    }
    
        public static String getJarContainingFolder(Class aclass) throws Exception {
          CodeSource codeSource = aclass.getProtectionDomain().getCodeSource();

          File jarFile;

          if (codeSource.getLocation() != null) {
            jarFile = new File(codeSource.getLocation().toURI());
          }
          else {
            String path = aclass.getResource(aclass.getSimpleName() + ".class").getPath();
            String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
            jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
            jarFile = new File(jarFilePath);
          }
          return jarFile.getParentFile().getAbsolutePath();
     }
    
	

       
}