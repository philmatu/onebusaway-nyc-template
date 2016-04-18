package org.onebusaway.nyc.core.model;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Philip Matuskiewicz
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

public class VehicleLocationRecord extends org.onebusaway.realtime.api.VehicleLocationRecord{

	//The core doesn't provide a VLR that has a run id.  MTABC buses require this to do scheduling comparisons since a trip has many runs.
	
	private static Logger _log = LoggerFactory.getLogger(VehicleLocationRecord.class);
	private static final long serialVersionUID = -4776307375244100918L;
	private String _runId = "";
	
	public String getRunId() {
		return _runId;
	}
	public void setRunId(String _runId) {
		this._runId = _runId;
	}
	
	public static void dump(VehicleLocationRecord vlr){
		_log.info("--------------------- VLR-------");
		_log.info("VID: "+vlr.getVehicleId());
		_log.info("TOR: "+vlr.getTimeOfRecord());
		_log.info("TOLU: "+vlr.getTimeOfLocationUpdate());
		_log.info("BID: "+vlr.getBlockId());
		_log.info("TID: "+vlr.getTripId());
		_log.info("SD: "+vlr.getServiceDate());
		_log.info("DAB: "+vlr.getDistanceAlongBlock());
		_log.info("CLAT: "+vlr.getCurrentLocationLat());
		_log.info("CLON: "+vlr.getCurrentLocationLon());
		_log.info("PHS: "+vlr.getPhase());
		_log.info("STAT: "+vlr.getStatus());
		_log.info("--------------------- END-VLR----");
	}
	
	public boolean makeRecordFromInferredLocationBean(NycQueuedInferredLocationBean irb) {
		
		//From other parts of OBA, the time values should be in Milliseconds
	    setVehicleId(AgencyAndId.convertFromString(irb.getVehicleId()));
	    setTimeOfRecord(irb.getRecordTimestamp());
	    setTimeOfLocationUpdate(irb.getRecordTimestamp());
	    setBlockId(AgencyAndIdLibrary.convertFromString(irb.getBlockId()));
	    setTripId(AgencyAndIdLibrary.convertFromString(irb.getTripId()));
	    if(irb.getServiceDate() == null){
	    	setServiceDate(0);
	    }else{
	    	setServiceDate(irb.getServiceDate());
	    }
	    setDistanceAlongBlock(irb.getDistanceAlongBlock());
	    setCurrentLocationLat(irb.getInferredLatitude().doubleValue());
	    setCurrentLocationLon(irb.getInferredLongitude().doubleValue());
	    setPhase(EVehiclePhase.valueOf(irb.getPhase()));
	    setStatus(irb.getStatus());
	    setRunId(irb.getRunId());

	    if(
	    	getVehicleId() != null &&
	    	getTimeOfRecord() != 0 &&
	    	getTimeOfLocationUpdate() != 0 &&
	    	getBlockId() != null &&
	    	getTripId() != null &&
	    	getServiceDate() != 0 &&
	    	getDistanceAlongBlock() != 0 &&
	    	getCurrentLocationLat() != 0 &&
	    	getCurrentLocationLon() != 0 &&
	    	getPhase() != null &&
	    	getStatus() != null &&
	    	getRunId() != null
	      ){
	    	return true;
	    }
		
		return false;
	}
	
	public VehicleLocationRecordBean getVehicleLocationRecordBean(){
		VehicleLocationRecordBean vlrb = new VehicleLocationRecordBean();

		vlrb.setVehicleId(getVehicleId().toString());
		vlrb.setTimeOfRecord(getTimeOfRecord());
		vlrb.setTimeOfLocationUpdate(getTimeOfLocationUpdate());
		if(getBlockId() != null){
			vlrb.setBlockId(getBlockId().toString());
		}
		if(getTripId() != null){
			vlrb.setTripId(getTripId().toString());
		}
		vlrb.setServiceDate(getServiceDate());
		vlrb.setDistanceAlongBlock(getDistanceAlongBlock());
		CoordinatePoint p = new CoordinatePoint(getCurrentLocationLat(), getCurrentLocationLon());
		vlrb.setCurrentLocation(p);
		if(getPhase() != null){
			vlrb.setPhase(getPhase().toString());
		}
		vlrb.setStatus(getStatus());
		
		return vlrb;
	}
	
}
