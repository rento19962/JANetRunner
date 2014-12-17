package org.keyser.anr.web.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.keyser.anr.core.AbstractCard;
import org.keyser.anr.core.AbstractCardLocationEvent;
import org.keyser.anr.core.AbstractCardRezzEvent;
import org.keyser.anr.core.AbstractCardTokenEvent;
import org.keyser.anr.core.EventMatcherBuilder;
import org.keyser.anr.core.EventMatchers;
import org.keyser.anr.core.FlowArg;
import org.keyser.anr.core.Game;

public class EventsBasedGameDtoBuilder {

	private final EventMatchers matchers = new EventMatchers();

	private Map<AbstractCard, CardDto> cards = new HashMap<>();

	private final Game game;

	public EventsBasedGameDtoBuilder(Game game) {
		match(AbstractCardLocationEvent.class, this::location);
		match(AbstractCardRezzEvent.class, this::rezzed);
		match(AbstractCardTokenEvent.class, this::tokens);

		this.game = game;
		this.game.bind(matchers);
	}

	private void location(AbstractCardLocationEvent evt) {
		AbstractCard card = evt.getPrimary();
		with(card, dto -> dto.setLocation(card.getLocation()));

	}

	private void with(AbstractCard card, FlowArg<CardDto> act) {
		CardDto dto = getOrCreate(card);
		act.apply(dto);
	}

	private void rezzed(AbstractCardRezzEvent evt) {
		AbstractCard card = evt.getPrimary();
		with(card, dto -> dto.setFace(card.isRezzed() ? CardDto.Face.up
				: CardDto.Face.down));
	}

	private void tokens(AbstractCardTokenEvent evt) {
		AbstractCard card = evt.getPrimary();
		with(card, dto -> dto.addToken(evt.getType(),
				card.getToken(evt.getType())));
	}

	private CardDto getOrCreate(AbstractCard card) {
		CardDto dto = cards.get(card);
		if (dto == null) {
			dto = new CardDto(card.getId());
			cards.put(card, dto);
		}
		return dto;
	}

	private <T> void match(Class<T> type, FlowArg<T> builder) {
		EventMatcherBuilder<T> match = EventMatcherBuilder.match(type,
				"@EventsBasedGameDtoBuilder");
		match.call(builder);
		matchers.add(match);
	}

	public GameDto build() {

		matchers.uninstall();
		GameDto dto = new GameDto();

		// TODO recopie des actions

		if (!cards.isEmpty()) {
			dto.setCards(new ArrayList<>(cards.values()));
		}

		return dto;
	}

}
