/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.Quartz;

import biz.szydlowski.sms.api.AutoLogCronApi;
import biz.szydlowski.sms.api.CronApi;
import biz.szydlowski.sms.setting.AutoLogCrontab;
import biz.szydlowski.sms.setting.Crontab;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 *
 * @author Dominik
 */
public class CronQuartz  {
    
   static final Logger logger =  LogManager.getLogger("biz.szydlowski.sms.CronQuartz");
   private boolean isDo = false;
  
   
   public void doJob()  {
        if (this.isDo)  {
             logger.fatal("CronQuartz is running");
             return;
         }
     
        this.isDo = true;
    
        try   {
           int queue = 0;
           Scheduler scheduler = new StdSchedulerFactory().getScheduler();
           List<JobDetail> jobs = new ArrayList<>();
           List<Trigger> triggers = new ArrayList<>(); 
         
           List<CronApi> cronsApi = new Crontab().getCronsApi();
       
           for(CronApi cronApi :cronsApi ){
                logger.info("Add crontab " + cronApi.getAlias());
                              
                jobs.add(JobBuilder.newJob(CronQuartzJob.class).withIdentity("CronQuartzJob." + queue, "CronQuartzJob").build());
                jobs.get(queue).getJobDataMap().put("cronApi", cronApi); 
                        
                triggers.add(TriggerBuilder.newTrigger().withIdentity("trigger." + queue, "CronQuartz")
                            .withSchedule(CronScheduleBuilder.cronSchedule(cronApi.getCronExpression()))
                            .build());

                queue++; 
           } 
           
           
           List<AutoLogCronApi> autoLogCronsApi = new AutoLogCrontab().getAutoLogCronsApi();
       
           for(AutoLogCronApi autoLogCronApi :autoLogCronsApi ){
                logger.info("Add crontab " + autoLogCronApi.getAlias());
                              
                jobs.add(JobBuilder.newJob(AutoLogQuartzJob.class).withIdentity("AutoCronQuartzJob." + queue, "AutoCronQuartzJob").build());
                jobs.get(queue).getJobDataMap().put("autoLogCronApi", autoLogCronApi); 
                        
                triggers.add(TriggerBuilder.newTrigger().withIdentity("trigger." + queue, "AutoCronQuartz")
                            .withSchedule(CronScheduleBuilder.cronSchedule(autoLogCronApi.getCronExpression()))
                            .build());

                queue++; 
           }
    
           if (!scheduler.isStarted())  {
               logger.info("Start scheduler");
               scheduler.start();
           }
           
           for (int ii=0; ii<jobs.size(); ii++){
                scheduler.scheduleJob(jobs.get(ii), triggers.get(ii));
            }
         }
         catch (SchedulerException e)     {
           logger.error(e);
         }
   }
  
   public boolean stop()  {
        try {
              Scheduler scheduler = new StdSchedulerFactory().getScheduler();
              scheduler.clear();
              scheduler.shutdown();
              isDo = false;
              return true;
         }  catch (SchedulerException e) {
           logger.error(e);
         }
         return false;
   }  
       
      
 }