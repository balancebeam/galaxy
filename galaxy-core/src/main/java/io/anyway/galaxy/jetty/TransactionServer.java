package io.anyway.galaxy.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;

import io.anyway.galaxy.spring.DataSourceAdaptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionServer {

	private static final int port = 3000;

	private Server server;

	private volatile boolean running = false;
	
	private DataSourceAdaptor dataSourceAdaptor = null;
	
	private static TransactionServer instance;
	
	
	public static TransactionServer instance(){
		if(instance == null){
			synchronized (TransactionServer.class) {
				if(instance == null){
					instance = new TransactionServer();
				}
			}
			
		}
		return instance;
	}
	
	

	private TransactionServer() {
		this.server = new Server(port);

		try {
			ServletContextHandler handler = new ServletContextHandler();
			handler.setContextPath("/");

			handler.setSessionHandler(new SessionHandler());

			handler.addServlet(EnvServlet.class, "/api/env");
			handler.addServlet(PropertiesServlet.class, "/api/props");
            handler.addServlet(TaskServlet.class, "/api/tasks");
            handler.addServlet(StaticContentServlet.class, "/*");


            server.setHandler(handler);
		} catch (Exception e) {
			log.error("Exception in building AdminResourcesContainer ", e);
		}
	}
	
	public DataSourceAdaptor getDataSource(){
		return this.dataSourceAdaptor;
	}
	
	public void setDataSource(DataSourceAdaptor dataSourceAdaptor ){
		this.dataSourceAdaptor = dataSourceAdaptor;
	}

	public void start() {

		if (!running) {
			try {
				server.start();
				running = true;
			} catch (Exception e) {
				log.error("Exception in Starting " + this.getClass().getSimpleName(), e);
			}
		}
	}

	public void shutdown() {
		if (running) {
			try {
				server.stop();
				running = false;
			} catch (Exception e) {
				log.error("Exception in Stopping " + this.getClass().getSimpleName(), e);
				if (!server.isStarted() && !server.isStarting()){
					running = false;
				}
			}
		}
	}

	public boolean isRunning() {
		return running;
	}

}
