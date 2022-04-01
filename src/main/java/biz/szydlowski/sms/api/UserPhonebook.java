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
public class UserPhonebook {
       
    private String username;
    private String phone;
    private final List<String> groups;
    
    
    public UserPhonebook(){
         username="";
         phone="";
         groups = new ArrayList<>();
    }
    
    public void setUsername(String _username){
        this.username=_username;
    }
    
    public void setPhone(String _phone){
        this.phone=_phone;
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
    
    
    public String getUsername(){
        return this.username;
    } 
    
    public String getPhone(){
        return this.phone;
    }
    
    public  List<String> getGroups(){
        return this.groups;
    }  
    
    public  String printGroupsPure(){
        StringBuilder sb = new StringBuilder();
        
        for (int i=0; i<groups.size();i++){
            sb.append(groups.get(i));
            if (groups.size()>1 && i<groups.size()-1) sb.append(";");
        }
        
        return sb.toString();
    }
    
    
    public  String printGroups(){
        StringBuilder sb = new StringBuilder();
        
        for (int i=0; i<groups.size();i++){
            sb.append(groups.get(i));
            if (groups.size()>1 && i<groups.size()-1) sb.append(" | ");
        }
        
        return sb.toString();
    }
    
}
