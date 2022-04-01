/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms.web;

/**
 *
 * @author szydlowskidom
 */
public class ApiJson {
    
   
    private String apiKey;
    private String username;
    private String message;
    private boolean base64;


    public String getApiKey() {
        return apiKey;
    } 
    
    public String getUsername() {
        return username;
    }
    
    public String getMessage() {
        return message;
    }
    public boolean isBase64() {
        return base64;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    } 
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public void setUsername(String user) {
        this.username = user;
    } 
    
    public void setBase64(boolean base64) {
        this.base64 = base64;
    }
 
}
