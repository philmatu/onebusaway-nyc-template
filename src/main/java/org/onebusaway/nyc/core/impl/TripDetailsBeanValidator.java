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

import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TripDetailsBeanValidator {
	
	private static Logger _log = LoggerFactory.getLogger(TripDetailsBeanValidator.class);
	
	private boolean _debug = false;
	public void setDebug(int i){
		if(i == 1){
			_debug = true;
		}
	}

	public boolean isValidTrip(TripDetailsBean currentTrip){
		if(currentTrip == null){
			if(_debug){
				_log.info("The current trip is unavailable.");
			}else{
				_log.debug("The current trip is unavailable.");
			}
			return false;
		}

		if(currentTrip.getStatus() == null){
			_log.debug("A trip status was unavailable.");
			return false;
		}

		if(currentTrip.getTrip() == null){
			_log.debug("A trip bean was unavailable for "+currentTrip.getStatus().getVehicleId()+".");
			return false;
		}

		if(currentTrip.getTrip().getRoute() == null){
			_log.debug("A route bean was unavailable for "+currentTrip.getStatus().getVehicleId()+".");
			return false;
		}

		if(currentTrip.getTrip().getRoute().getId() == null){
			_log.debug("A route id was unavailable for "+currentTrip.getStatus().getVehicleId()+".");
			return false;
		}
		
		if(currentTrip.getSchedule() == null){
			_log.debug("A schedule was unavailable for "+currentTrip.getStatus().getVehicleId()+".");
			return false;
		}
		
		if(currentTrip.getSchedule().getStopTimes() == null){
			_log.debug("No stop times for "+currentTrip.getStatus().getVehicleId()+".");
			return false;
		}
		
		if(currentTrip.getSchedule().getStopTimes().size() < 1){
			_log.error("The trip schedule is too small (< 1) for trip: "+currentTrip.getTripId()+" vehicle "+currentTrip.getStatus().getVehicleId()+".");
			return false;
		}
		
		return true;
	}

}
