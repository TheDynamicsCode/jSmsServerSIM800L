/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.tasks;

import static biz.szydlowski.sms.JDaemonSms.functions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Dominik
 */
public class HotSmsRead extends Thread {
        static final Logger logger =  LogManager.getLogger( HotSmsRead.class);
       
        @Override
        public void run() {
            Thread.currentThread().setName("HotReadTask");
            logger.debug("HotSmsRead run");
            functions.smsReader();
        }
}

