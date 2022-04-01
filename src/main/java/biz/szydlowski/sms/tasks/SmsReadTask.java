/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.tasks;

import static biz.szydlowski.sms.JDaemonSms.functions;
import static biz.szydlowski.sms.WorkingObjects.lastSMSReader;
import java.util.TimerTask;

/**
 *
 * @author Dominik
 */
     public class SmsReadTask extends TimerTask {
          
            @Override
            public void run() {
                 //Thread.currentThread().setName("SmsReadTask");
                 lastSMSReader = System.currentTimeMillis();
                 functions.smsReader(); 
             }
      
            
     } 