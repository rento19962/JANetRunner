package org.keyser.anr.core.corp.jinteki;

import org.keyser.anr.core.CardDef;
import org.keyser.anr.core.Cost;
import org.keyser.anr.core.Faction;
import org.keyser.anr.core.corp.Ice;
import org.keyser.anr.core.corp.IceType;

@CardDef(name = "Cell Portal", oid = "01074")
public class CellPortal extends Ice {
	public CellPortal() {
		super(Faction.JINTEKI.infl(2), Cost.credit(5), IceType.CODEGATE, 7);
	}
}
