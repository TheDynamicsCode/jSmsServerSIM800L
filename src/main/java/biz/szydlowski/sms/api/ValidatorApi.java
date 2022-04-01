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
public class ValidatorApi {
    
    String FormPhoneStartsWith;
    int FormPhoneMaxLength;
    int FormPhoneMinLength;
    
    public ValidatorApi(){
         FormPhoneStartsWith="";
         FormPhoneMaxLength=10;
         FormPhoneMinLength=2;    
    }
    
  
    public void setFormPhoneStartsWith(String set){
        FormPhoneStartsWith=set;
    }
    
    public void setFormPhoneMaxLength(String set){
        FormPhoneMaxLength=Integer.parseInt(set);
    } 
    
    public void setFormPhoneMinLength(String set){
        FormPhoneMinLength=Integer.parseInt(set);
    }
    
   
    public String getFormPhoneStartsWith(){
        return FormPhoneStartsWith;
    }
    
    public int getFormPhoneMaxLength(){
       return FormPhoneMaxLength;
    } 
    
    public int getFormPhoneMinLength(){
        return FormPhoneMinLength;
    }  

    
}
