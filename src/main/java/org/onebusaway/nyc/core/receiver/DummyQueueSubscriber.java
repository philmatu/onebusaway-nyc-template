package org.onebusaway.nyc.core.receiver;

import org.onebusaway.nyc.core.interfaces.TripDetailsSubscriber;
import org.onebusaway.nyc.core.model.VehicleLocationRecord;

public class DummyQueueSubscriber implements TripDetailsSubscriber{

	@Override
	public void publish(VehicleLocationRecord vlr) {
		System.out.println(vlr);
	}

}
