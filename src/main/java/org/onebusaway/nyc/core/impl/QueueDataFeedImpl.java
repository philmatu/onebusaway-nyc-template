package org.onebusaway.nyc.core.impl;

/**
 * 
 * @author Philip Matuskiewicz
 * 
 * Copyright 2013-2014 Metropolitan Transportation Authority (New York City Transit) - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.List;
import org.onebusaway.nyc.core.interfaces.TripDetailsSubscriber;
import org.onebusaway.nyc.core.model.VehicleLocationRecord;
import org.onebusaway.nyc.core.queue.InferenceQueueListenerTask;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class QueueDataFeedImpl extends InferenceQueueListenerTask implements ApplicationListener<ContextRefreshedEvent> {
	
	protected static Logger _log = LoggerFactory.getLogger(QueueDataFeedImpl.class);
	
	private String _localHost = null;
	private Integer _localPort = null;
	private String _localName = null;
	private boolean _initialized = false;

	@Autowired
	private ConfigurationService _configurationService;
	
	@Autowired
	private TripDetailsBeanValidator _tripDetailsBeanValidator;
	
	@Autowired
	private List<TripDetailsSubscriber> _subscribers;
	
	@Autowired
	private NycTransitDataService _nyctds;
	
	@Autowired
	private BundleManagementService _bundleManagementService;
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// don't attempt to process messages until context is initialized; problems will happen
		_initialized = true;
	}

	//allow setting via spring framework for the queue to listen to for POST IE, also allows junit testing
	public void setHost(String h) {
		_localHost = h;
	}

	public void setPort(Integer p) {
		_localPort = p;
	}

	public void setName(String n) {
		_localName = n;
	}

	@Override
	public String getQueueHost() {
		if (_localHost != null){
			_log.warn("Using Application Context override value for Queue Host "+_localHost);
			return _localHost;
		}

		return _configurationService.getConfigurationValueAsString("tds.inputQueueHost", null);
	}

	@Override
	public String getQueueName() {
		if (_localName != null){
			//This gets called every time a new message comes in, so it's ideal to have no debug output
			return _localName;
		}

		return _configurationService.getConfigurationValueAsString("tds.inputQueueName", null);
	}

	@Override
	public Integer getQueuePort() {
		if (_localPort != null){
			_log.warn("Using Application Context override value for Queue Port "+_localPort);
			return _localPort;
		}

		return _configurationService.getConfigurationValueAsInteger("tds.inputQueuePort", 1234);
	}
	
	@Override
	protected void processResult(NycQueuedInferredLocationBean inferredResult, String contents) {
		if(_initialized){
			VehicleLocationRecord vlr = new VehicleLocationRecord();//using an override of the VLR
			vlr.makeRecordFromInferredLocationBean(inferredResult);
			
			if(!_bundleManagementService.bundleIsReady()){
				_log.info("Bundle isn't ready, throwing out record: "+vlr);
				return;
			}
			
			//tell NYC TDS about this vehicle
			VehicleLocationRecordBean vlrb = vlr.getVehicleLocationRecordBean();
			_nyctds.submitVehicleLocation(vlrb);
			
			TripDetailsBean trip = getCurrentTrip(vlrb.getVehicleId(), vlrb.getTripId(), vlr.getTimeOfLocationUpdate());
			
			//this happens if the vehicle is at base or in a deadhead, since there isn't a valid trip
			if(!_tripDetailsBeanValidator.isValidTrip(trip)){
				_log.debug("Trip isn't valid, pieces were likely missed in the TDS.  VLR: "+vlr);
				return;
			}
			
			for(TripDetailsSubscriber s : _subscribers){
				s.publish(vlr);
			}
			
		}
	}

	@Override
	public String getQueueDisplayName() {
		return "inference_queue";
	}
	
	private TripDetailsBean getCurrentTrip(String vehicleId, String tripId, long lastupdatetime){
		if(vehicleId == null || tripId == null){
			return null;
		}
		//get the trip details for this trip
		TripDetailsQueryBean query = new TripDetailsQueryBean();
		query.setTripId(tripId);
		query.setVehicleId(vehicleId);
		query.setTime(lastupdatetime);
		return _nyctds.getSingleTripDetails(query);
	}
}
