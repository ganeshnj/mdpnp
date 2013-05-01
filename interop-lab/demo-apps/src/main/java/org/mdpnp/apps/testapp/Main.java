package org.mdpnp.apps.testapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.mdpnp.messaging.Binding;
import org.mdpnp.messaging.BindingFactory;
import org.mdpnp.messaging.BindingFactory.BindingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	
	
	private static final Logger log = LoggerFactory.getLogger(Main.class);
	public static void main(String[] args) throws Exception {
	    System.setProperty("java.net.preferIPv4Stack","true");

	    
	    Configuration runConf = null;
	    
	    File jumpStartSettings = new File(".JumpStartSettings");
	    
	    boolean cmdline = false;
	    
	    if(args.length > 0) {
	        runConf = Configuration.read(args);
	        cmdline = true;
	    } else if(jumpStartSettings.exists() && jumpStartSettings.canRead()) {
	        FileInputStream fis = new FileInputStream(jumpStartSettings);
	        runConf = Configuration.read(fis);
	        fis.close();
	    }

	    Configuration writeConf = null;
	    
		if(!cmdline) {
		    ConfigurationDialog d = new ConfigurationDialog(runConf);
		    runConf = d.showDialog();
		    // It's nice to be able to change settings even without running
		    if(null == runConf) {
		        writeConf = d.getLastConfiguration();
		    }
		} else {
		    // fall through to allow configuration via a file
		}
		
		if(null != runConf) {
		    writeConf = runConf;
		}
		
		if(null != writeConf) {
            if(!jumpStartSettings.exists()) {
                jumpStartSettings.createNewFile();
            }
            
            
            if(jumpStartSettings.canWrite()) {
                FileOutputStream fos = new FileOutputStream(jumpStartSettings);
                writeConf.write(fos);
                fos.close();
            }
		}
		
		if(null != runConf) {

		    
		    
		    BindingType binding = runConf.getBinding();
		    String bindingSettings = runConf.getBindingSettings();
		    
		    switch(binding) {
		    case RTI_DDS:
		        try {
        	        if(!(Boolean)Class.forName("org.mdpnp.rti.dds.DDS").getMethod("init").invoke(null)) {
        	            throw new Exception("Unable to init");
        	        }
		        } catch (Throwable t) {
		            log.warn("Unable to initialize RTI DDS, falling back to JGroups transport", t);
		            binding = BindingType.JGROUPS;
		            bindingSettings = "";
		        }
		        break;
		        
		    }

			switch(runConf.getApplication()) {
			case ICE_Device_Interface:
			    DeviceAdapter.start(runConf.getDeviceType(), binding, bindingSettings, runConf.getAddress(), !cmdline);
				break;
			case ICE_Supervisor:
			    DemoApp.start(binding, bindingSettings);
			    break;
			}
		} else if(cmdline) {
		    Configuration.help(Main.class, System.out);
		}
		
	}
}
