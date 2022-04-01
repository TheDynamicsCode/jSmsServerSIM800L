/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.web;


import static biz.szydlowski.sms.WorkingObjects.adminConn;
import static biz.szydlowski.sms.WorkingObjects.apiConn;
import biz.szydlowski.utils.MonitorThread;
import biz.szydlowski.utils.RejectedExecutionHandlerImpl;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author dominik
 */
public class SmsWebServer extends Thread {
   
    protected int serverPort   = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean isStopped    = false;
    protected Thread runningThread = null;
    private final int maxConnectionCount = 20;
      
    static final Logger logger =  LogManager.getLogger(SmsWebServer.class);
    private ThreadPoolExecutor executorPool;
    
    public SmsWebServer (int port){
        this.serverPort = port;
    }

    @Override
    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
            this.runningThread.setName("WebServer");
        }
        try {
            openServerSocket();
        } catch (Exception e){
            isStopped = true;
            logger.error(e);
            
        }   
        
        RejectedExecutionHandlerImpl rejectionHandler = new RejectedExecutionHandlerImpl();
                
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        //creating the ThreadPoolExecutor
        executorPool = new ThreadPoolExecutor(2, maxConnectionCount, 300, 
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), threadFactory, rejectionHandler);
        executorPool.allowCoreThreadTimeOut(true);
         //start the monitoring thread
        MonitorThread monitor = new MonitorThread(executorPool, 60);
        Thread monitorThread = new Thread(monitor);
        monitorThread.setName("monitorThread");
        monitorThread.start();
                
        while(!isStopped()){ 
            boolean api=false; 
            boolean admin=false;
            Socket clientSocket = null;
            
            try {
               clientSocket = this.serverSocket.accept();
               clientSocket.setKeepAlive(true);
            
               String address = clientSocket.getInetAddress().getHostAddress();
              
                              
               Iterator<String> iteratorweb = apiConn.iterator();
              
               while(iteratorweb.hasNext()){
                    String obj = iteratorweb.next();
                    if (obj.equals("all")){
                         api=true;
                         break;
                    } else {
                        if (obj.startsWith("*") || obj.endsWith("*")){
                            //gwiazdka
                            Pattern r = Pattern.compile(obj);
                            if ( r.matcher(address).find()){
                              api=true;
                              break;
                            } 
                            
                        } else {
                             if (obj.equalsIgnoreCase(address)){
                                 api=true;
                                 break;
                             }
                        }
                       
                    }
                }
                
                Iterator<String> iteratoradmin = adminConn.iterator();
                while(iteratoradmin.hasNext()){
                    String obj = iteratoradmin.next();
                    if (obj.equals("all")){
                         admin=true;
                         break;
                    } else {
                        if (obj.startsWith("*") || obj.endsWith("*")){
                            //gwiazdka
                            Pattern r = Pattern.compile(obj);
                            if ( r.matcher(address).find() ){
                              admin=true;
                              break;
                            } 
                            
                        } else {
                             if (obj.equalsIgnoreCase(address) ){
                                 admin=true;
                                 break;
                             }
                        }
                       
                    }
                }
                //A client has connected to this server. Send welcome message
            } catch (IOException e) {
                if(isStopped()) {
                    logger.info("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }   
           
            executorPool.execute(new Thread(new ServerWorkerRunnable(clientSocket, api, admin)));        
           
        }
        logger.info("Server Stopped.") ;
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }
   
    public void stopSever() {
        this.isStopped = true;
        try {
            logger.info("Currently active threads: " + Thread.activeCount());
            interruptAll();
            this.serverSocket.close();
        } catch (IOException e) {
            logger.error("Error closing server", e);
        }
    }

    
   private void interruptAll(){
       executorPool.shutdown();
   }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
    }

}

