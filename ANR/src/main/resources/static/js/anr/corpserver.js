define([ "mix", "layout/package", "ui/package", "geometry/package", "layout/impl/anchorlayout", "layout/impl/flowlayout", "./cardcontainerBox" ],// 
function(mix, layout, ui, geom, AnchorLayout, FlowLayout, CardContainerBox) {

	/**
	 * Permet d'apparaitre différement dans le debugger
	 */
	function Ices(layoutManager) {
		layout.AbstractBoxContainer.call(this, layoutManager, { horizontal : true }, new FlowLayout({ direction : FlowLayout.Direction.TOP, padding : 3 }));
	}
	mix(Ices, layout.AbstractBoxContainer);

	function Upgrades(layoutManager) {
		layout.AbstractBoxContainer.call(this, layoutManager, {}, new FlowLayout({ padding : 0, direction : FlowLayout.Direction.RIGHT, spacing : -30 }));
	}
	mix(Upgrades, layout.AbstractBoxContainer);

	function CorpServer(layoutManager, def) {
		this.def = def;
		layout.AbstractBoxContainer.call(this, layoutManager, {}, new FlowLayout({ direction : FlowLayout.Direction.TOP, align : FlowLayout.Align.MIDDLE,
			padding : 3 }));

		var innerLayout = null
		if (def.id >= -3)
			innerLayout = new AnchorLayout({});
		else
			innerLayout = new FlowLayout({ padding : 0, direction : FlowLayout.Direction.RIGHT, spacing : -30 });

		var type = "Remote " + (-def.id - 3);
		if (def.id == -1)
			type = "Archives";
		else if (def.id == -2)
			type = "R&D";
		if (def.id == -3)
			type = "HQ";

		this.mainContainer = new CardContainerBox(layoutManager, type, innerLayout);

		this.upgrades = new Upgrades(layoutManager);
		this.ices = new Ices(layoutManager);

		var upgradesMinSize = new layout.AbstractBoxContainer(layoutManager, {}, new AnchorLayout({ padding : 3, minSize : new geom.Size(80, 111) }));
		upgradesMinSize.addChild(this.upgrades);

		this.addChild(upgradesMinSize);
		this.addChild(this.mainContainer);
		this.addChild(this.ices);
	}

	mix(CorpServer, layout.AbstractBoxContainer);
	mix(CorpServer, function() {
	});

	return CorpServer;
});