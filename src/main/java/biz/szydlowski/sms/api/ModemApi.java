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
public class ModemApi {
   
    private String port_name_master;
    private String port_name_first_slave;
    private String port_name_second_slave;
    private int port_timeout;
    private int  port_data_rate;
    private String AT_CSCA;
    
    public  ModemApi (){
        port_name_master="";
        port_name_first_slave="";
        port_name_second_slave="";
        port_timeout = 2000;
        port_data_rate = 9600;
        AT_CSCA="";
    }
 
    
    public void setPortNameMaster(String set){
        this.port_name_master=set;
    }
    
    public void setPortNameFirstSlave(String set){
        this.port_name_first_slave=set;
    }
    
    public void setPortNameSecondSlave(String set){
        this.port_name_second_slave=set;
    } 
    
    public void setAT_CSCA(String set){
        this.AT_CSCA=set;
    }   
    
    public void setPortTimeout(String set){
        this.port_timeout=Integer.parseInt(set);
    }
    
    public void setPortDataRate(String set){
        this.port_data_rate=Integer.parseInt(set);
    }
    
    public String getPortNameMaster(){
        return this.port_name_master;
    }
    
    public String getPortNameFirstSlave(){
        return this.port_name_first_slave;
    }
    
    public String getPortNameSecondSlave(){
        return this.port_name_second_slave;
    } 
    
    public String getAT_CSCA(){
        return this.AT_CSCA;
    }   
    
    public int getPortTimeout(){
        return this.port_timeout;
    }
    
    public int getPortDataRate(){
        return this.port_data_rate;
    }
    
    
}
