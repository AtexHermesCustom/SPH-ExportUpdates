package com.atex.h11.custom.sph.export.update;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final String loggerName = Main.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Properties props = new Properties();
        String inputFilePath = null;
        File inputFile = null;
        
        logger.entering(loggerName, "main");
        
        try {
            /* Gather command line parameters.
             * p - properties file
             * i - input file
             */
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-p")) {
                    props.load(new FileInputStream(args[++i]));
                }
                else if (args[i].startsWith("-p")) {
                    props.load(new FileInputStream(args[i].substring(2)));
                }
                else if (args[i].equals("-i")) {
                    inputFilePath = args[++i];
                }
                else if (args[i].startsWith("-i")) {
                    inputFilePath = args[i].substring(2);
                }
            }
            
            if (inputFilePath == null) {
                throw new Exception("No input file passed.");
            }
            inputFile = new File(inputFilePath);
            
            // process the xml file
            XMLProcessor proc = new XMLProcessor(props, inputFile);
            proc.Start();
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "", e);
            System.exit(1);            
        }
         
        logger.exiting(loggerName, "main");
        System.exit(0);
    }
}
