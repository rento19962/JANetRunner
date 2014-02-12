package org.keyser.anr.core.runner;

import java.util.Collection;
import java.util.Collections;

import org.keyser.anr.core.CardLocation;
import org.keyser.anr.core.Cost;
import org.keyser.anr.core.Influence;

public abstract class Hardware extends InstallableRunnerCard {

	public Hardware(Influence influence, Cost cost) {
		super(influence, cost);
	}
	
	@Override
	public Collection<CardLocation> possibleInstallPlaces() {
		return Collections.singletonList(CardLocation.HARDWARES);
	}

}
