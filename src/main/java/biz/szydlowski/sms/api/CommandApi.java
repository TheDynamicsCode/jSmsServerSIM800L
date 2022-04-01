/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;

/**
 *
 * @author Dominik
 */
public class CommandApi {
    

    private String gsm_alias="";
    private String command="";
    private String response="";
    private String response_error="";
    private boolean loginRequired=true;
    private final List<String> groups = new ArrayList<>();
    private HashMap<String, String> user_command_map = new HashMap<String, String>();
    private HashMap<String, String> user_response = new HashMap<String, String>();
    private HashMap<String, String> user_response_error = new HashMap<String, String>();
     
    public void setCommandMapForUser(String user, String command){
        user_command_map.put(user, command);
    }
    
    public void setResponseForUser(String user, String res){
        user_response.put(user, res);
    }
    
    public void setResponseErrorForUser(String user, String res){
        user_response_error.put(user, res);
    }
    
    public String getCommandForUser(String user){
        return ObjectUtils.defaultIfNull(user_command_map.get(user), command);
    }
    
    public String getResponseForUser(String user){
        return ObjectUtils.defaultIfNull( user_response.get(user), response);
    }
    
     public String getResponseErrorForUser(String user){
        if (response_error.length()==0) response_error=response;
        return ObjectUtils.defaultIfNull( user_response_error.get(user), response_error);
    }
    
    public void setGsmAlias(String _gsm_alias){
        this.gsm_alias=_gsm_alias;
    }
    
    public void setCommand(String _command){
        this.command=_command;
    }  
    
    public void setLoginRequired(String _lr){
        this.loginRequired=Boolean.parseBoolean(_lr);
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
        groups.addAll(Arrays.asList(__groups));
    }
    
   public  String getGsmAias(){
        return this.gsm_alias;
   }   
   
   public  String getCommand(){
        return this.command;
   }  
   
   public boolean getLoginRequired(){
        return this.loginRequired;
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

    /**
     * @return the response
     */
    public String getResponse() {
        return response;
    }

    /**
     * @param response the response to set
     */
    public void setResponse(String response) {
        this.response = response;
    }

    /**
     * @return the response_error
     */
    public String getResponse_error() {
        if (response_error.length()==0) response_error=response;
        return response_error;
    }

    /**
     * @param response_error the response_error to set
     */
    public void setResponse_error(String response_error) {
        this.response_error = response_error;
    }
    
}
