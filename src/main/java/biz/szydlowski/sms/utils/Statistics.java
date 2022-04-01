/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.utils;

import biz.szydlowski.utils.OSValidator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author dominik
 */
public class  Statistics {
    private final Properties statistics = new Properties();
    private boolean wasLoad=false;
   
    private String _setting="setting/stats";
   
    static final Logger logger =  LogManager.getLogger("biz.szydlowski.sms.utils.Statistics");
    
    
    public Statistics  (){
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
              _setting = absolutePath + "/" + _setting;
        }
    }

    public void loadStats(){
        
        InputStream input = null;
        File stFile = new File(_setting);
        try { 
            stFile.createNewFile();
        } catch (IOException ex) {
            logger.error("Nie utworzono pliku do statystyk.");
        } 	

	try {

		input = new FileInputStream(_setting);

		// load a properties file
		statistics.load(input);
                
                wasLoad=true;


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

    }
    
    public String readStatsToString(){
        
        StringBuilder sb = new StringBuilder();
        
        try (BufferedReader br = new BufferedReader(new FileReader(_setting))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(" \n");
            }
        } catch (IOException ex) {
            logger.error(ex);
        }
        
        return sb.toString();

    } 
    
    public String readStatsToHTMLString(){
        
        StringBuilder sb = new StringBuilder();
        
         if (!wasLoad) {
              sb.append("Statistics wasn't load!!!!!!!!");
              
         } else {
             
              sb.append("<style>\n table, th, td {\n border: 1px solid black;\n border-collapse: collapse;\n }\n th, td {\n padding: 5px;\n");
              sb.append(" text-align: left;  \n}\n </style>");  
              sb.append("<table style=\"width:50%\">");    
              sb.append("<tr>\n <th>KEY</th>\n <th>VALUE</th>\n </tr>");
                  
              Set<Object> keys = getAllKeys(statistics );
              keys.stream().map((k) -> (String)k).map((key) -> {
                      String value = getPropertyValue(statistics , key);
                      sb.append("<tr><td>");
                      sb.append(key);
                      sb.append("</td>");
                      sb.append("<td>");
                      sb.append(value);
                    return key;
                }).map((_item) -> {
                    sb.append("</td>");
                    return _item;
                }).forEachOrdered((_item) -> {
                    sb.append("</tr>");
                }); 
              
              sb.append("</table><br/>");  
               keys.clear();
             
         }
        
        return sb.toString();

    }
    
    public void writeStats(){
        OutputStream output = null;
  	
        try {

		output = new FileOutputStream( _setting);

		// save properties to project root folder
		statistics.store(output, null);

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
    
    public void disasterSmsReadCountPlus(){
          if (!wasLoad) {
              logger.error("Statistics wasn't load!!!!!!!!");
              return;
          }
          int tmp =0;
          try {
             tmp =Integer.parseInt(statistics.getProperty("disaster.count.smsread", "0"));
          } catch (NumberFormatException ignore){}
          statistics.setProperty("disaster.count.smsread",  ""+tmp);
          writeStats();
    }  
    
    public void disasterMaintenanceCountPlus(){
         if (!wasLoad) {
              logger.error("Statistics wasn't load!!!!!!!!");
              return;
          }
          int tmp =0;
          try {
              tmp =Integer.parseInt(statistics.getProperty("disaster.count.maintenance", "0"));
          } catch (NumberFormatException ignore){}            
          tmp++;
          statistics.setProperty("disaster.count.maintenance",  ""+tmp);
          writeStats();
    }
  
    public void disasterUserSessionCountPlus(){
          if (!wasLoad) {
              logger.error("Statistics wasn't load!!!!!!!!");
              return;
          }
          int tmp =0;
          try {
              tmp =Integer.parseInt(statistics.getProperty("disaster.count.us", "0"));
          } catch (NumberFormatException ignore){}            
          tmp++;
          statistics.setProperty("disaster.count.us",  ""+tmp);
          writeStats();
    }
   
    public void disasterSerialReadCountPlus(){
          if (!wasLoad) {
              logger.error("Statistics wasn't load!!!!!!!!");
              return;
          }
          int tmp =0;
          try {
             tmp =Integer.parseInt(statistics.getProperty("disaster.count.sr", "0"));
          } catch (NumberFormatException ignore){}            
          tmp++;
          statistics.setProperty("disaster.count.sr",  ""+tmp);
          writeStats();
    }
    
    public void disasterSerialWriteCountPlus(){
          if (!wasLoad) {
              logger.error("Statistics wasn't load!!!!!!!!");
              return;
          }
          int tmp =0;
          try {
             tmp =Integer.parseInt(statistics.getProperty("disaster.count.sw", "0"));
          } catch (NumberFormatException ignore){}            
          tmp++;
          statistics.setProperty("disaster.count.sw",  ""+tmp);
          writeStats();
    }
   
    public void disasterSerialPortNullCountPlus(){
          if (!wasLoad) {
              logger.error("Statistics wasn't load!!!!!!!!");
              return;
          }
          int tmp =0;
          try {
             tmp =Integer.parseInt(statistics.getProperty("disaster.count.spn", "0"));
          } catch (NumberFormatException ignore){}            
          tmp++;
          statistics.setProperty("disaster.count.spn",  ""+tmp);
          writeStats();
    }
    
    public void disasterIOErrorCountPlus(){
          if (!wasLoad) {
              logger.error("Statistics wasn't load!!!!!!!!");
              return;
          }
          int tmp =0;
          try {
              tmp =Integer.parseInt(statistics.getProperty("disaster.count.io", "0"));
          } catch (NumberFormatException ignore){}
                      
          tmp++;
          statistics.setProperty("disaster.count.io",  ""+tmp);
          writeStats();
    }
    
    public void runsPlus(){
         SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss"); 
          int runs = 0;
          try {
              runs = Integer.parseInt(statistics.getProperty("runs", "0"));
          } catch (NumberFormatException ignore){}
             
          runs++;
          statistics.setProperty("runs", ""+runs);
          statistics.setProperty("current.start.time", sdf.format(new Date()));
          writeStats();
    }
    
    public long getTotalUptimeSec(){
        return Long.parseLong(statistics.getProperty("total.uptime_sec", "0"));
    }
    
    public long getTotalAtSend(){
        return Long.parseLong(statistics.getProperty("total.at.sends", "0"));
    }  
    
    public long getTotalAtError(){
        return Long.parseLong(statistics.getProperty("total.at.error", "0"));
    }
    
    public long getTotalAtOK(){
        return Long.parseLong(statistics.getProperty("total.at.ok", "0"));
    }
   
    public long getTotalSmsReads(){
        return Long.parseLong(statistics.getProperty("total.sms.reads", "0"));
    }
     
    public long getTotalSmsSends(){
        return Long.parseLong(statistics.getProperty("total.sms.sends", "0"));
    }
    
    public void setProperty(String key, String value){
        if (!wasLoad) logger.error("Statistics wasn't load!!!!!!!!");
        statistics.setProperty(key, value);
    }     
  
    
    public int getMaxJavaUsedMemory(){
        return Integer.parseInt(statistics.getProperty("total.java.max.used.memory.mb", "0"));
    }   
    
    private Set<Object> getAllKeys(Properties prop){
            Set<Object> keys = prop.keySet();
            return keys;
    }

    private String getPropertyValue(Properties prop, String key){
            return prop.getProperty(key);
    }  
   
    public Properties getStatistics(){
        if (!wasLoad) logger.error("Statistics wasn't load!!!!!!!!");
        return statistics;
    } 
    
    
}
