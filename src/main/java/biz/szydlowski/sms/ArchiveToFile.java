package biz.szydlowski.sms;


import biz.szydlowski.utils.OSValidator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArchiveToFile {
  static final Logger logger =  LogManager.getLogger("biz.szydlowski.sms.ArchiveToFile");
  static final String archive = "sms_archive";

  private static String getCurrentTimeStamp()
  {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Date now = new Date();
    String strDate = sdf.format(now);
    return strDate;
  }

  private static String getTimeName() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");

    Date now = new Date();
    String strDate = sdf.format(now);
    return strDate;
  }

  public void addToRepository(String filename, String text)
  {
    saveToFile(filename, text);
  }

  private void mkDirs(String data)  {

    String absolutePath = "";

    if (OSValidator.isUnix()) {
      absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
      absolutePath = new StringBuilder().append("/").append(absolutePath.substring(0, absolutePath.lastIndexOf("/"))).append("/").toString();
    }

    try  { 
      boolean bool = false;
      File f  = new File(new StringBuilder().append(absolutePath).append("sms_archive").toString());

      if (!f.exists()) {
        bool = f.mkdirs();
      }

      File f1 = new File(new StringBuilder().append(absolutePath).append("sms_archive").append("/").append(data).toString());
      if (!f1.exists()) {
        bool = f1.mkdirs();
      }

    }
    catch (Exception e)
    {
      logger.error(e);
    }
  }

  private void saveToFile(String filename, String text)
  {
    String datePart = getTimeName();
    mkDirs(datePart);
    
    if (filename.equalsIgnoreCase("sms_outbound_txt.out")){
        int len = text.length();
        int ile = (int) Math.ceil(len/120.0);
        StringBuilder sb = new StringBuilder();
        sb.append(text).append("\t").append(len).append("\t").append(ile);
        text = sb.toString();
    }

    String absolutePath = "";

    if (OSValidator.isUnix()) {
      absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
      absolutePath = new StringBuilder().append("/").append(absolutePath.substring(0, absolutePath.lastIndexOf("/"))).append("/").toString();
    }

    String sFile = new StringBuilder().append(absolutePath).append("sms_archive").append("/").append(datePart).append("/").append(filename).toString();

    File f1 = new File(sFile);
    if (f1.exists()){
      try
      {
        StringBuilder old = new StringBuilder();
        FileReader fr = new FileReader(sFile);
       // Throwable localThrowable3 = null;
        try { 
            try (BufferedReader br = new BufferedReader(fr)) {
                String s;
                while ((s = br.readLine()) != null) {
                    old.append(s).append(System.getProperty("line.separator"));
                } 
            }
          fr.close();
        } catch (IOException e) {
              logger.error(e);
        }
        finally
        {
          if (fr != null)  try {
              fr.close();
          } catch (IOException e) {
              logger.error(e);
          } else fr.close();
        }
        try {
            FileWriter fw = new FileWriter(sFile); 
            fw.write(old.toString());
            fw.write(new StringBuilder().append(getCurrentTimeStamp()).append(" ").append(text).append(System.getProperty("line.separator")).toString());
            fw.flush();
            fw.close();
        } catch (IOException exc2)   {   
           logger.error("Caught IOException: ", exc2);
        }
      }
      catch (IOException exc)
      {
        logger.error("Caught IOException: ", exc);
      }
  } else
      try  {
       FileWriter fw = new FileWriter(sFile); 
       fw.write(new StringBuilder().append(getCurrentTimeStamp()).append(" ").append(text).append(System.getProperty("line.separator")).toString());
       fw.flush();
       fw.close();
      }
      catch (IOException exc) {
        logger.error("Caught IOException: ", exc);
      }
  }
}