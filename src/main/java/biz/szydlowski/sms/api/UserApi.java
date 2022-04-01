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
public class UserApi {
       
    private String username;
    private String phone;
    private boolean admin; 
    private boolean redirectUnknownSms;
    private boolean activeAtStartup;
    private final List<String> groups;
    private long timeout_minutes;
    
    public UserApi(){
        username="";
        phone="";        
        admin=false; 
        redirectUnknownSms=false;
        activeAtStartup=false;
        groups = new ArrayList<>();
        timeout_minutes=100L;
    }
    
    
    public void setUsername(String _username){
        this.username=_username;
    }
    
    public void setPhone(String _phone){
        this.phone=_phone;
    } 
    
    public void setAdmin(String _admin){
       this.admin=Boolean.parseBoolean(_admin);
    }
   
    public void setAdmin(boolean _admin){
        this.admin=_admin;
    }
   
    public void setActiveAtStartup(String _activeAtStartup){
        this.activeAtStartup=Boolean.parseBoolean(_activeAtStartup);
    } 
    
        
    public void setActiveAtStartup(boolean _activeAtStartup){
        this.activeAtStartup=_activeAtStartup;
    }   
    
    public void setRedirectUnknownSms(String _redirectUnknownSms){
        this.redirectUnknownSms=Boolean.parseBoolean(_redirectUnknownSms);
    } 
    
    public void setRedirectUnknownSms(boolean _redirect){
        this.redirectUnknownSms=_redirect;
    }
   
    public void addGroup(String _group){
        this.groups.add(_group);
    } 
    public void setGroup(String _group){
        this.groups.clear();
        this.groups.add(_group);
    } 
    
    public void setGroups(String _groups){
        String [] __groups = _groups.split(";");
        this.groups.clear();
        groups.addAll(Arrays.asList(__groups));
    }
    
    public void setTimeoutInMinutes(String timeout){
        try {
            this.timeout_minutes=Long.parseLong(timeout);
        } catch (NumberFormatException ignore){}
    }
   
    public void setTimeoutInMinutes(long timeout){
        this.timeout_minutes=timeout;
    }
    
    public String getUsername(){
        return this.username;
    } 
    
    public String getPhone(){
        return this.phone;
    }
    
    public boolean isAdmin(){
        return this.admin;
    }
    
    public boolean isRedirectUnknownSms(){
        return this.redirectUnknownSms;
    }
     
    public boolean isActiveAtStartup(){
        return this.activeAtStartup;
    }
   
    public  List<String> getGroups(){
        return this.groups;
    }  
    
    public  String printGroups(){
        StringBuilder sb = new StringBuilder();
        
        for (int i=0; i<groups.size();i++){
            sb.append(groups.get(i));
            if (groups.size()>1 && i<groups.size()-1) sb.append(" | ");
        }
        
        return sb.toString();
    }  
    
    public  String printGroupsPure(){
        StringBuilder sb = new StringBuilder();
        
        for (int i=0; i<groups.size();i++){
            sb.append(groups.get(i));
            if (groups.size()>1 && i<groups.size()-1) sb.append(";");
        }
        
        return sb.toString();
    }
    
    public long getTimeoutInMinutes(){
       return this.timeout_minutes;
    }
}
