package org.keyser.anr.core.corp;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keyser.anr.core.AbstractAbility;
import org.keyser.anr.core.CoreAbility;
import org.keyser.anr.core.Cost;
import org.keyser.anr.core.Event;
import org.keyser.anr.core.Flow;
import org.keyser.anr.core.Game;
import org.keyser.anr.core.NotificationEvent;
import org.keyser.anr.core.PlayableUnit;
import org.keyser.anr.core.Player;
import org.keyser.anr.core.WalletCredits;
import org.keyser.anr.core.WinCondition;

public class Corp extends PlayableUnit {

	/**
	 * Le click de base
	 * 
	 * @author PAF
	 * 
	 */
	class ClickForCredit extends CoreAbility {
		ClickForCredit() {
			super("click-for-credit", Cost.action(1));
		}

		@Override
		public void apply() {
			Optional<WalletCredits> wc = getWallet().wallet(WalletCredits.class);
			wc.ifPresent(WalletCredits::add);

			Game game = getGame();

			game.notification(NotificationEvent.CORP_CLICKED_FOR_CREDIT.apply());
			game.apply(new CorpClickForCredit(), next);
		}
	}

	/**
	 * Le click pour piocher
	 * 
	 * @author PAF
	 * 
	 */
	class ClickForDraw extends CoreAbility {
		ClickForDraw() {
			super("click-for-draw", Cost.action(1));
		}

		@Override
		public void apply() {
			getGame().notification(NotificationEvent.CORP_CLICKED_FOR_DRAW.apply());
			draw(next);
		}
	}

	/**
	 * Les 3 clicks pour purger
	 * 
	 * @author PAF
	 * 
	 */
	class ClickForPurge extends CoreAbility {
		ClickForPurge() {
			super("click-for-purge", Cost.action(3));
		}

		@Override
		public boolean isEnabled() {

			// renvoi vrai s'il y a des virus chez le runner
			return getGame().getRunner().hasVirus();
		}

		@Override
		public void apply() {
			Game game = getGame();
			game.notification(NotificationEvent.CORP_CLICKED_FOR_PURGE.apply());
			game.getRunner().purgeVirus(next);
		}
	}

	/**
	 * L'evenement la Corp a piocher
	 * 
	 * @author PAF
	 * 
	 */
	public static class CorpCardDraw extends Event {
		private final CorpCard card;

		public CorpCardDraw(CorpCard card) {
			this.card = card;
		}

		public CorpCard getCard() {
			return card;
		}
	}

	/**
	 * L'�venement la corp � pris un credit
	 * 
	 * @author PAF
	 * 
	 */
	public static class CorpClickForCredit extends Event {

	}

	private CorpArchivesServer archive = new CorpArchivesServer(this);

	private int badPub = 0;

	private CorpHQServer hq = new CorpHQServer(this);

	private CorpRDServer rd = new CorpRDServer(this);

	private Map<Integer, CorpRemoteServer> remotes;

	/**
	 * Renvoi toutes les abilites utilisables du noyau
	 * 
	 * @return
	 */
	@Override
	protected void addAllAbilities(List<AbstractAbility> a) {
		a.add(new ClickForCredit());
		a.add(new ClickForDraw());
		a.add(new ClickForPurge());
	}

	public void addToRD(CorpCard c) {
		rd.add(c);
	}

	/**
	 * Pioche une carte
	 * 
	 * @param next
	 */
	public void draw(Flow next) {
		List<CorpCard> rds = getRd().getCards();
		Game game = getGame();
		if (!rds.isEmpty()) {
			CorpCard c = rds.remove(0);
			getHq().add(c);

			game.notification(NotificationEvent.CORP_DRAW.apply());
			game.apply(new CorpCardDraw(c), next);
		} else {
			// fin de la partie corp à perdu
			game.setResult(WinCondition.CORP_BUST);
		}

	}

	public CorpArchivesServer getArchive() {
		return archive;
	}

	public int getBadPub() {
		return badPub;
	}

	public CorpHQServer getHq() {
		return hq;
	}

	@Override
	public PlayableUnit getOpponent() {
		return getGame().getRunner();
	}

	@Override
	public Player getPlayer() {
		return Player.CORP;
	}

	public CorpRDServer getRd() {
		return rd;
	}

	public void remove(CorpRemoteServer crs) {
		remotes.remove(crs.getId());
	}

	public void setArchive(CorpArchivesServer archive) {
		this.archive = archive;
	}

	public Corp setBadPub(int badPub) {
		this.badPub = badPub;
		return this;
	}

	public void setHq(CorpHQServer hq) {
		this.hq = hq;
	}

	public void setRd(CorpRDServer rd) {
		this.rd = rd;
	}

}
