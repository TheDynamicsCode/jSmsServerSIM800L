/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.sms;

import java.io.IOException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 *
 * @author szydlowskidom
 */
public class ScriptExecutor {
    
    private int iExitValue;
    private String sCommandString;
    private String sCommandReturn;
    static final Logger logger =  LogManager.getLogger(ScriptExecutor.class);
    private int response_code;
    
    public void runScript(String command){
        
        sCommandString = command;
        CommandLine oCmdLine = CommandLine.parse(sCommandString);
        DefaultExecutor oDefaultExecutor = new DefaultExecutor();
        oDefaultExecutor.setExitValue(0);
        response_code=0;
        try {
            iExitValue = oDefaultExecutor.execute(oCmdLine);
            sCommandReturn = "Executed (" + iExitValue + ").";
            response_code=0;
        } catch (ExecuteException e) {
            logger.error("Execution failed " + e);
            sCommandReturn = "Execution failed. (" + iExitValue + ").";
            logger.error(e);
            response_code=1;
        } catch (IOException e) {
            sCommandReturn = "Permission denied. (" + iExitValue + ").";
            logger.error("Permission denied. " + e);
            response_code=2;
        }
        
    }
    
    public String getCommandReturn(){
        return sCommandReturn;
    }

    /**
     * @return the response_code
     */
    public int getResponse_code() {
        return response_code;
    }

    /**
     * @param response_code the response_code to set
     */
    public void setResponse_code(int response_code) {
        this.response_code = response_code;
    }
        
    
}
