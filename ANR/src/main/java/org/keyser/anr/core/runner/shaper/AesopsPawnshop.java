package org.keyser.anr.core.runner.shaper;

import org.keyser.anr.core.CardDef;
import org.keyser.anr.core.Cost;
import org.keyser.anr.core.Faction;
import org.keyser.anr.core.runner.Resource;

@CardDef(name = "Aesop's Pawnshop", oid = "01047")
public class AesopsPawnshop extends Resource {

	public AesopsPawnshop() {
		super(Faction.SHAPER.infl(2), Cost.credit(1));
	}
}