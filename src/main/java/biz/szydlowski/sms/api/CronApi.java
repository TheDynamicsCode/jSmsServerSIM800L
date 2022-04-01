/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Dominik
 */
public class CronApi  {
       
    private String alias="";
    private boolean sendOnlyToLoginUsers=false;
    private String message="msg";
    private final List<String> data = new ArrayList<>();
    private String cronExpression="0 0/5 0-5 ? * MON-FRI";
    private boolean isGroup=false;
    private boolean isUser=false;
     
    public void setAlias(String set){
        this.alias=set;
    }
    
    public void setMessage(String set){
        this.message=set;
    } 
    
    public void setOnlyToLoginUsers(String set){
       this.sendOnlyToLoginUsers=Boolean.parseBoolean(set);
    }

     
    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setIsGroup() {
        this.isGroup = true;
        this.isUser = false;
    }

    public boolean isUser() {
        return isUser;
    }

    public void setIsUser() {
        this.isUser = true;
        this.isGroup = false;
    }
    
    public void setUsersOrGroups(String sdata){
        
        String [] __data = sdata.split(";");
        this.data.clear();
        data.addAll(Arrays.asList(__data));
        
        
    }
    
    public List<String> getUsersOrGroups() {
        return  data;
    }

    
    public String getAlias(){
        return this.alias;
    } 
    
    public String getMessage(){
        return this.message;
    }
    
    public boolean isSendOnlyToLoginUsers(){
        return this.sendOnlyToLoginUsers;
    }
    
 
    public  String printUsersOrGrup(){
        StringBuilder sb = new StringBuilder();
        
        for (int i=0; i<data.size();i++){
            sb.append(data.get(i));
            if (data.size()>1 && i<data.size()-1) sb.append(" | ");
        }
        
        return sb.toString();
    }  
    
    public  String printUsersorGroupPure(){
        StringBuilder sb = new StringBuilder();
        
        for (int i=0; i<data.size();i++){
            sb.append(data.get(i));
            if (data.size()>1 && i<data.size()-1) sb.append(";");
        }
        
        return sb.toString();
    }
      
       
    
}