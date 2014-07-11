package org.notebook.services;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.http.channel.client.AuthManager;
import org.http.channel.proxy.CaptchaItem;
import org.http.channel.proxy.CaptchaStatus;
import org.http.channel.proxy.RemoteStatus;
import org.notebook.cache.NoteBook;
import org.notebook.io.HttpResponse;
import org.notebook.io.SimpleHttpClient;

public class CaptchaClient {
	private Log log = LogFactory.getLog("gate");
	public static final String XAUTH = "X-proxy-auth";
		
	//public String status = DISCONNECTED;
	public RemoteStatus status = new RemoteStatus();
	public AuthManager auth = null;
	public boolean isRunning = false;
	private Collection<CaptchaListener> statusListeners = new ArrayList<CaptchaListener>(); 
	//private ThreadPoolExecutor proxyWorkerPool = null;
	private ThreadPoolExecutor proxyCommandPool = null;
	private Timer timer = new Timer();
	private long commandCount = 0;
	
	public NoteBook config = null;
	
	private CaptchaListener listenerProxy = new CaptchaListener(){
		@Override
		public void syncRouteStatus(RemoteStatus r) {
			for(CaptchaListener l: statusListeners){
				l.syncRouteStatus(r);
			}
		}

		@Override
		public void findCaptcha(CaptchaItem item) {
			for(CaptchaListener l: statusListeners){
				l.findCaptcha(item);
			}			
		}

		@Override
		public void updateStatus(CaptchaStatus status) {
			for(CaptchaListener l: statusListeners){
				l.updateStatus(status);
			}			
		}};
	
		
	public void start(NoteBook book, ThreadPoolExecutor pool){
		config = book;
		proxyCommandPool = new ThreadPoolExecutor(
				2, 5, 60, TimeUnit.SECONDS, 
				new LinkedBlockingDeque<Runnable>(50)
				);
		
		log.info("Starting sync service...");
		if(book != null && book.proxy != null && book.proxy.trim().toLowerCase().startsWith("http://")){
			log.info("http proxy:" + book.proxy);
			System.setProperty("HTTP_PROXY", book.proxy);
			try {
				URL proxy = new URL(book.proxy);
				System.setProperty("http.proxyHost", proxy.getHost());
				System.setProperty("http.proxyPort", proxy.getPort() + "");
			} catch (MalformedURLException e) {
				log.error(e.toString(), e);
			}
		}
		
		timer.scheduleAtFixedRate(new TrackerScheduler(), 100, 60 * 1000);
	}
	
	/**
	 * 手动强制连接服务。
	 */
	public void connect(){
		new TrackerScheduler().run();
	}
		
	public void addStatusListener(CaptchaListener l){
		this.statusListeners.add(l);
	}
	public void removeStatusListener(CaptchaListener l){
		this.statusListeners.remove(l);
	}
	
	/**
	 * 判断需要多少个下载命令的线程。
	 * @author deon
	 */
	class TrackerScheduler extends TimerTask {
		@Override
		public void run() {
			if(proxyCommandPool.getActiveCount() < 2) {
				proxyCommandPool.execute(new RequestTracker());
			}else if(commandCount > 10 && proxyCommandPool.getActiveCount() < 5){
				proxyCommandPool.execute(new RequestTracker());
			}
			log.info(String.format("Active thread:%s, executed proxy command count:%s", 
					proxyCommandPool.getActiveCount(),
					commandCount));
			commandCount = 0;
		}
	}
	
	/**
	 * 用来从远程服务器，下载需要请求的HTTP命令。
	 * @author deon
	 */
	class RequestTracker implements Runnable{
		@Override
		public void run() {
			ObjectInputStream ios = null;
			HttpURLConnection connection = null;
			try {				
				URL request = new URL(config.endpoint + "/~/request?name=" + config.user + "&g=" + config.name);
				log.debug("connecting to " + request.toString());
				connection = (HttpURLConnection )request.openConnection();
				connection.setReadTimeout(1000 * 60 * 5);
				connection.setConnectTimeout(1000 * 30);
				
				connection.addRequestProperty(XAUTH, config.password);
				log.debug("xauth key:" + config.password);
				
				connection.setRequestMethod("POST");
				connection.setDoInput(true);
				connection.connect();
				
				ios = new ObjectInputStream(connection.getInputStream());
				for(Object obj = ios.readObject(); obj != null; obj = ios.readObject()){
					commandCount++;
					log.debug("read object:" + obj);
					if(obj instanceof CaptchaItem){
						status.requestCount++;
						//log.debug("Request:" + obj.toString());
						listenerProxy.findCaptcha((CaptchaItem)obj);
					}else if(obj instanceof RemoteStatus){
						log.debug("status:" + obj.toString());
						status.copyFrom((RemoteStatus)obj);
						status.updated();
						listenerProxy.syncRouteStatus(status);
					}else if(obj instanceof CaptchaStatus){
						listenerProxy.updateStatus((CaptchaStatus)obj);				
					}
				}				
			}catch(ConnectException conn){
				log.info(String.format("Failed connection to '%s', msg:%s", config.endpoint, conn.toString()));
				synchronized(status){
					status.connection = RemoteStatus.DISCONNECTED;
					status.updated();
					listenerProxy.syncRouteStatus(status);
				}
			}catch(MalformedURLException e){
				log.info(String.format("Invalid remote url:" + config.endpoint));				
			}catch(IOException eof){
				log.info(String.format("EOF read proxy command. msg:%s", eof.toString()));
				if(commandCount == 0){
					status.connection = "ConnectFailed";
					listenerProxy.syncRouteStatus(status);
				}
			}catch (Exception e) {
				log.error(e.toString(), e);
			} finally {
				if (ios != null)
					try {
						ios.close();
					} catch (IOException e) {
						log.error(e.toString(), e);
					}
				if (connection != null) connection.disconnect();
			}
		}
	}
	
	public void responseCaptcha(String sid, String code, int padding){
		
		log.info(String.format("Response sid:" + sid + ", code:" + code + ", pading:" + padding));			

		Map<String, String> param = new HashMap<String, String>();
		param.put("sid", sid);
		param.put("code", code);
		param.put("g", config.name);
		param.put("name", config.user);
		param.put("padding", padding + "");

		try {
			
			URL request = new URL(config.endpoint + "/~/reponse");
			
			HttpResponse resp = SimpleHttpClient.post(request, param);
			//InputStream in = ClientHttpRequest.post(request, param);
			//JSONParser parser = new JSONParser();
			
			//List<Category> result = new ArrayList<Category>();
			//Map<String, Object> jsonObject = (Map<String, Object>)parser.parse(new InputStreamReader(in, "utf-8"));			
			//log.info(String.format("Response status:" + resp.toString() + "\n msg:" + resp.getResponseMessage()));			
		}catch(Exception e){

			log.error(e.toString(), e);
		}
		
	}
	

}
