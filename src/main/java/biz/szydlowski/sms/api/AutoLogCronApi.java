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
public class AutoLogCronApi {

  
    private final List<String> data = new ArrayList<>();
    private String cronExpression="0 0/5 0-5 ? * MON-FRI";
    private boolean isGroup=false;
    private boolean isUser=false;
    private String actionType="logout";
    private String alias="";

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
   
    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
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

     
    
     
}
