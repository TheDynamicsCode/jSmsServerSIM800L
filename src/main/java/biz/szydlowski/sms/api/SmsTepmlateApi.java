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
public class SmsTepmlateApi {
    

    private String name;
    private String message;
    private final List<String> groups;
    
    public SmsTepmlateApi(){
         name="";
         message="";
         groups = new ArrayList<>();
    }
    
    
    public void setName(String set){
        this.name=set;
    }
    
    public void setMessage(String set){
        this.message=set;
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
    
   public  String getName(){
        return this.name;
   }   
   
   public  String getMessage(){
        return this.message;
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
    
    
    
}
