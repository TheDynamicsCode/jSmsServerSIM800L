/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.api;

/**
 *
 * @author Dominik
 */
public class ServerApi  {
   
    private boolean QUARTZ_ENABLED = true;
    private int web_port = 8080;
    private String restart_script="";
    private String apiKey="";
    private String sms_package_expire_date="";
    private int MaxInboundMsgWeb=20;
    private int MaxOutboundMsgWeb=20;
    private int webMaxConnectionCount=20;
    private int atRretryCount=4;
    private int atErrorThreshold=3;
    private int maxSmsInHour=10;

    public int getMaxSmsInHour() {
        return maxSmsInHour;
    }

    public void setMaxSmsInHour(String maxSmsHour) {
        this.maxSmsInHour =Integer.parseInt(maxSmsHour);
        if (this.maxSmsInHour<1) this.maxSmsInHour=1;
    }
            
    public void setWebPort(String set){
        this.web_port=Integer.parseInt(set);
    }
   
    public void setApiKey(String set){
        this.apiKey=set;
    }  
    
    public void setAtErrorThreshold(String set){
        this.atErrorThreshold=Integer.parseInt(set);
        if (this.atErrorThreshold<4) this.atErrorThreshold=4;
    }
      
    public void setAtRretryCount(String set){
        this.atRretryCount=Integer.parseInt(set);
        if (this.atRretryCount<2) this.atRretryCount=2;
    }
     
    public void setRestartScript(String set){
        this.restart_script=set;
    }  
    
    public void setSmsPackageExpireDate(String set){
        this.sms_package_expire_date=set;
    }
    
    public void setQuartzEnabled(String set){
       QUARTZ_ENABLED = Boolean.parseBoolean(set);
   }
    
   public void setMaxInboundMsgWeb(String set){
        MaxInboundMsgWeb=Integer.parseInt(set);
    }
      
      
    public void setMaxOutboundMsgWeb(String set){
        MaxOutboundMsgWeb=Integer.parseInt(set);
    } 
    
    public void setWebMaxConnectionCount(String set){
          try {
              webMaxConnectionCount=Integer.parseInt(set);
          } catch (NumberFormatException e){}
    } 
    
     public int getWebPort(){
       return this.web_port;
    }
    
    public String getRestartScript(){
        return this.restart_script;
    }  
    
    public String getApiKey(){
        return this.apiKey;
    }    
    
    public String getSmsPackageExpireDate(){
        return this.sms_package_expire_date;
    }   
    
    public boolean getQuartzEnabled(){
      return QUARTZ_ENABLED;
   }
       
    
    public int getMaxInboundMsgWeb(){
       return MaxInboundMsgWeb;
    }
      
      
    public int  getMaxOutboundMsgWeb(){
        return MaxOutboundMsgWeb;
    }
    
    public int getWebMaxConnectionCount(){
          return webMaxConnectionCount;
    } 
    
    public int getAtRretryCount(){
        return atRretryCount;
    }
  
    public int getAtErrorThreshold(){
        return atErrorThreshold;
    }
    
    
}