package org.keyser.anr.web.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keyser.anr.core.CardLocation;
import org.keyser.anr.core.PlayerType;
import org.keyser.anr.core.TokenType;

public class CardDto {

	public enum Face {
		down, up
	}

	private List<ActionDto> actions;

	private Face face;

	private PlayerType faction;

	private int id;

	private CardLocation location;

	private Map<String, Integer> tokens;

	private String url;

	public CardDto() {
	}

	public CardDto(int id) {
		this.id = id;
	}
	
	public void addToken(TokenType type, int value){
		if(tokens==null)
			tokens=new LinkedHashMap<String, Integer>();
		
		tokens.put(type.name().toLowerCase(), value);
		
	}

	public List<ActionDto> getActions() {
		return actions;
	}

	public void setActions(List<ActionDto> actions) {
		this.actions = actions;
	}

	public Face getFace() {
		return face;
	}

	public void setFace(Face face) {
		this.face = face;
	}

	public PlayerType getFaction() {
		return faction;
	}

	public void setFaction(PlayerType faction) {
		this.faction = faction;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public CardLocation getLocation() {
		return location;
	}

	public void setLocation(CardLocation location) {
		this.location = location;
	}

	public Map<String, Integer> getTokens() {
		return tokens;
	}

	public void setTokens(Map<String, Integer> tokens) {
		this.tokens = tokens;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
