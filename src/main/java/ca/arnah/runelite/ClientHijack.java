package ca.arnah.runelite;

import net.runelite.client.RuneLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arnah
 * @since Nov 07, 2020
 */
public class ClientHijack{
	
	public ClientHijack(){
		System.out.println("Client hijacked");
		new Thread(()->{
			while(RuneLite.getInjector() == null){
				try{
					Thread.sleep(100);
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
			System.out.println("Injector found");
			RuneLite.getInjector().getInstance(HijackedClientBackup.class).start();
			Logger logger = LoggerFactory.getLogger(ClientHijack.class);
			logger.info("finished ClientHijack");
		}).start();
	}
}