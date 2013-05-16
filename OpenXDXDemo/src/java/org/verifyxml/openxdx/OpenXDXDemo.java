package org.verifyxml.openxdx;

import com.oracle.openxdx.XDXFactory;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.transform.stream.StreamResult;

/**
 *
 * @author serega
 */
@WebService(serviceName = "OpenXDXService")
public class OpenXDXDemo {
    // Defines JNDI name for Data source
    private static String PMIX_DATASOURCE_CONTEXT = "jdbc/PMIX";

    // OpenXDX variables
    private static final String OPEN_XDX_DUMP_XML = "-open-xdx-dump.xml";
    private static final String OPEN_XDX_XML = "-open-xdx.xml";

    //CAM Templates
    private static final String PERSON_QUERY_TEMPLATE =
        "/PMPRequest-Person-Query.cxf";
    private static final String PRESCRIPTION_QUERY_TEMPLATE =
        "/PMPPrescriptionReport-ResultsQueryPerson.cxf";
    private File personTemplateFile;
    private File pmixTemplateFile;


    // Data source
    private DataSource datasourcePMIX;

    public OpenXDXDemo() {

        // Lookup and set DataSource
        try {
            Context initialContext = new InitialContext();
            if (initialContext == null) {
                System.out.println("JNDI problem. Cannot get InitialContext.");
            }
            datasourcePMIX = (DataSource)initialContext.lookup(PMIX_DATASOURCE_CONTEXT);
            personTemplateFile = extractResource(PERSON_QUERY_TEMPLATE);
            pmixTemplateFile = extractResource(PRESCRIPTION_QUERY_TEMPLATE);

        } catch (NamingException ex) {
            Logger.getLogger(OpenXDXDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @WebMethod
    @WebResult(name = "PersonResult")
    public String doPersonQuery(@WebParam(name = "applicantID") Integer applicantID) {
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("$IDnumber", Integer.toString(applicantID));
        return getOpenXDX(personTemplateFile, tokens);
    }

    @WebMethod
    @WebResult(name = "PMIXResult")
    public String doPMIXQuery(@WebParam(name = "personName")
        String personName) {
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("$familyName", personName);
        return getOpenXDX(pmixTemplateFile, tokens);
    }

    private String getOpenXDX(File cxfFile, Map<String, String> tokens) {
        String openXDXSource = null;
        File dataOutput = null;
        File openXDXFile = null;

        try {
            // Get new Processor
            com.oracle.openxdx.processor.Processor processor = XDXFactory.newProcessor();

            // Define data output and final result files
            dataOutput =
                    File.createTempFile(this.getClass().getName(), OPEN_XDX_DUMP_XML);

            openXDXFile =
                    File.createTempFile(this.getClass().getName(), OPEN_XDX_XML);

            // Set CAM Template
            processor.setTemplate(cxfFile);

            // Set Data source
            processor.setDataSource(datasourcePMIX);

            // Extract data
            processor.extract(dataOutput, tokens);

            // Generate Open-XDX XML
            processor.generate(dataOutput, new StreamResult(openXDXFile));

            // Return Open-XDX as a String
            openXDXSource =
                    new Scanner(openXDXFile).useDelimiter("\\Z").next();

        } catch (Exception ex) {
            Logger.getLogger(OpenXDXDemo.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if(dataOutput != null)
                dataOutput.deleteOnExit();
            if(openXDXFile != null)
                openXDXFile.deleteOnExit();
        }
        return openXDXSource;
    }

    private File extractResource(String resourceName) {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile(this.getClass().getName(), ".cxf");
            // read resource into InputStream
            InputStream inputStream =
                this.getClass().getResourceAsStream(resourceName);

            // write the inputStream to a FileOutputStream
            OutputStream out = new FileOutputStream(tmpFile);

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            inputStream.close();
            out.flush();
            out.close();
        
        } catch (IOException ex) {
            Logger.getLogger(OpenXDXDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return tmpFile;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            personTemplateFile.deleteOnExit();
            pmixTemplateFile.deleteOnExit();
        } finally {
            super.finalize();
        }
    }
}
