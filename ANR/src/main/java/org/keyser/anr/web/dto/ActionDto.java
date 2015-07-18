package org.keyser.anr.web.dto;

import java.util.List;

import org.keyser.anr.core.PlayerType;
import org.keyser.anr.core.UserDragActionTo;

public class ActionDto {

	private int id;

	private String text;

	private String cost;

	private List<VariableCostDto> costs;

	private String cls;

	private PlayerType faction;

	private String type;

	private Boolean enableDrag;
	
	private  List<UserDragActionTo> dragTo;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getCost() {
		return cost;
	}

	public void setCost(String cost) {
		this.cost = cost;
	}

	public String getCls() {
		return cls;
	}

	public void setCls(String cls) {
		this.cls = cls;
	}

	public PlayerType getFaction() {
		return faction;
	}

	public void setFaction(PlayerType faction) {
		this.faction = faction;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<VariableCostDto> getCosts() {
		return costs;
	}

	public void setCosts(List<VariableCostDto> costs) {
		this.costs = costs;
	}

	public Boolean getEnableDrag() {
		return enableDrag;
	}

	public void setEnableDrag(Boolean enableDrag) {
		this.enableDrag = enableDrag;
	}

	public List<UserDragActionTo> getDragTo() {
		return dragTo;
	}

	public void setDragTo(List<UserDragActionTo> dragTo) {
		this.dragTo = dragTo;
	}

}
