package org.onebusaway.nyc.core.queue;

import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.queue.DNSResolver;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * Base class for listeners that subscribe to ZeroMQ. Provides a simple
 * re-connection mechanism if the IP changes.
 */
//TODO push this out to org/onebusaway/nyc/queue replacing the old QueueListenerTask class
public abstract class QueueListenerTask {

	protected static Logger _log = LoggerFactory
			.getLogger(QueueListenerTask.class);
	@Autowired
	protected ConfigurationService _configurationService;
	@Autowired
	private ThreadPoolTaskScheduler _taskScheduler;
	private ExecutorService _executorService = null;
	protected boolean _initialized = false;
	protected ObjectMapper _mapper = new ObjectMapper();
	protected DNSResolver _resolver = null;
	protected int _countInterval = 10000;

	public abstract boolean processMessage(String address, byte[] buff);

	public abstract void startListenerThread();

	public abstract String getQueueHost();

	public abstract String getQueueName();

	/**
	 * Return the name of the queue for display of statistics in logs.
	 */
	public abstract String getQueueDisplayName();

	public abstract Integer getQueuePort();

	public void setCountInterval(int countInterval) {
		this._countInterval = countInterval;  
	}

	//if set to true, the class will bind to the socket, this is good for testing
	protected boolean _bind = false;
	public void setBindSocket(boolean bind){
		_bind = bind;
	}

	public void startDNSCheckThread() {
		String host = getQueueHost();
		_resolver = new DNSResolver(host);
		if (_taskScheduler != null) {
			DNSCheckThread dnsCheckThread = new DNSCheckThread();
			// ever 10 seconds
			_taskScheduler.scheduleWithFixedDelay(dnsCheckThread, 10 * 1000);
		}
	}

	private class SubscribingThread implements Runnable {

		int processedCount = 0;
		private Context context;
		private Socket subscribeSocket;
		private boolean init = false;
		private Date markTimestamp = new Date();

		//Runs when class is instantiated to configure the subscriber
		public SubscribingThread(String endpoint, String topic){
			context = ZMQ.context(1);
			subscribeSocket = context.socket(ZMQ.SUB);
			subscribeSocket.setReceiveTimeOut(2500);//times out after 2.5 seconds of no activity
			if(endpoint.contains("*")){
				subscribeSocket.bind(endpoint);
			}else{
				subscribeSocket.connect(endpoint);	
			}
			subscribeSocket.subscribe(topic.getBytes(ZMQ.CHARSET));//subscribe to everything
		}

		public boolean isRunning(){
			return init;
		}

		public void run(){
			init = true;
			while (!Thread.currentThread().isInterrupted() && !_executorService.isShutdown()) {
				try{
					String recepient = subscribeSocket.recvStr();
					byte[] message = null;
					if(subscribeSocket.hasReceiveMore()){
						message = subscribeSocket.recv();
					}
					if(recepient == null || message == null){
						continue;
					}
					processMessage(recepient, message);
				}catch(ZMQException e){
					_log.info("ZMQ subscriber interrupted");
					break;
				}
				if (processedCount > _countInterval) {
					long timeInterval = (new Date().getTime() - markTimestamp.getTime()); 
					_log.info(getQueueDisplayName()
							+ " input queue: processed " + _countInterval + " messages in "
							+ (timeInterval/1000) 
							+ " seconds. (" + (1000.0 * processedCount/timeInterval) 
							+ ") records/second");

					markTimestamp = new Date();
					processedCount = 0;
				}
			}
			subscribeSocket.close();
			context.term();
			_log.info("ZMQ Subscriber Thread is dead");
		}
	}

	@PostConstruct
	public void setup() {
		_executorService = Executors.newFixedThreadPool(1);
		startListenerThread();
		startDNSCheckThread();
		_log.warn("threads started for queue " + getQueueName());
	}

	@PreDestroy
	public void destroy() {
		_executorService.shutdownNow();
	}

	protected void reinitializeQueue() {
		try {
			initializeQueue(getQueueHost(), getQueueName(), getQueuePort());
		} catch (InterruptedException ie) {
			return;
		}
	}

	// (re)-initialize ZMQ with the given args
	protected synchronized void initializeQueue(String host, String queueName,
			Integer port) throws InterruptedException {
		String bind = "tcp://" + host + ":" + port;
		_log.warn("binding to " + bind + " with topic=" + queueName);

		SubscribingThread subscriberThread = new SubscribingThread(bind, queueName);
		_executorService.execute(subscriberThread);
		//wait for subscriber to be operational
		while(!subscriberThread.isRunning()){
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				throw new RuntimeException("An interrupted exception was caught in an improper place", e);
			}
		}

		_log.warn("queue " + queueName + " is listening on " + bind);
		_initialized = true;

	}

	private class DNSCheckThread extends TimerTask {

		@Override
		public void run() {
			try {
				if (_resolver.hasAddressChanged()) {
					_log.warn("Resolver Changed -- re-binding queue connection");
					reinitializeQueue();
				}
			} catch (Exception e) {
				_log.error(e.toString());
				_resolver.reset();
			}
		}
	}

}