package org.keyser.anr.core;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.mapping;
import static org.keyser.anr.core.RecursiveIterator.recurse;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keyser.anr.core.corp.Corp;
import org.keyser.anr.core.corp.CorpCard;
import org.keyser.anr.core.corp.CorpServer;
import org.keyser.anr.core.runner.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Game implements Notifier, ConfigurableEventListener {

	abstract class AbstractDiscardPhaseEvent extends AbstractTurnEvent {
		private int maxHandsize = 5;

		public int getMaxHandsize() {
			return maxHandsize;
		}

		public void setMaxHandsize(int maxHandsize) {
			this.maxHandsize = maxHandsize;
		}
	}

	abstract class AbstractReinitAction extends AbstractTurnEvent {

		private int action;

		private final PlayableUnit active;

		private AbstractReinitAction(PlayableUnit active, int action) {
			this.active = active;
			this.action = action;
		}

		public int getAction() {
			return action;
		}

		public PlayableUnit getActive() {
			return active;
		}

		public void setAction(int action) {
			this.action = action;
		}

		void setupAction() {
			getActive().getWallet().wallet(WalletActions.class, wa -> wa.setAmount(action));
		}
	}

	abstract class AbstractTurnEvent extends Event {
	}

	public class CorpDiscardEvent extends AbstractDiscardPhaseEvent {
	}

	public class CorpReinitActionEvent extends AbstractReinitAction {
		CorpReinitActionEvent() {
			super(corp, 3);
		}
	}

	public class CorpStartOfTurnEvent extends AbstractTurnEvent {
	}

	public class CorpTurnEndedEvent extends AbstractTurnEvent {
	}

	public class GameStartedEvent extends AbstractTurnEvent {
	}

	/**
	 * Permet de gerer les agenda
	 * 
	 * @author PAF
	 * 
	 */
	public class PingPong {

		private final Map<Player, Boolean> done = new EnumMap<>(Player.class);

		private Flow next;

		private PingPong(Flow next) {
			this.next = next;
			init();
		}

		/**
		 * Peremet de verifier ce qu'il faut faire quand une ability à été joué
		 * 
		 * @param triggered
		 * @param current
		 * @param replay
		 *            le meme joueur garde la priorite
		 * @param toNextPlayer
		 *            passse au joueur suivant
		 */
		private void checkAction(AbstractAbility triggered, PlayableUnit current, Flow replay, Flow toNextPlayer) {
			// si pas d'action on zappe, ou si action de base
			if (triggered == null) {
				done.put(current.getPlayer(), true);

				// si tout le monde a confirmé
				if (done.values().stream().allMatch(b -> b)) {
					next.apply();
				} else
					toNextPlayer.apply();

			} else if (triggered instanceof CoreAbility) {
				toNextPlayer.apply();
			} else {

				// on remet le compteur à zero
				init();
				replay.apply();
			}
		}

		public void corp() {
			trigger(corp, triggered -> checkAction(triggered, corp, this::corp, this::runner));
		}

		private void init() {
			done.put(Player.CORP, false);
			done.put(Player.RUNNER, false);
		}

		public void runner() {
			trigger(runner, triggered -> checkAction(triggered, runner, this::runner, this::corp));
		}

		/**
		 * Analyse des abilities
		 * 
		 * @param unit
		 * @param triggeredFlow
		 */
		private void trigger(PlayableUnit unit, FlowArg<AbstractAbility> triggeredFlow) {
			Player player = unit.getPlayer();
			Wallet wallet = unit.getWallet();

			Stream<AbstractAbility> s = unit.getAbilities();
			Stream<AbstractAbility> affordable = s.filter(p -> p.isAffordable(wallet));

			// s'il y a une action du noyau, on ne rajoute pas none car c'est le
			// tour du joueur
			AbstractAbility[] it = affordable.toArray(i -> new AbstractAbility[i]);
			boolean mayRezz = false;

			// TODO gestion des abilites de rezz (qui peuvent ne peut pas être
			// payable, mais le runner ne doit pas le savoir !!)
			Question q = ask(player, NotificationEvent.WHICH_ABILITY);

			for (AbstractAbility aa : it) {
				// on enregistre les actions
				if (aa instanceof SingleAbility) {
					SingleAbility sa = (SingleAbility) aa;

					// on enregistre la question
					sa.register(q, wallet, () -> triggeredFlow.apply(sa));
				}
			}

			// on rajoute l'absence d'action uniquement en cas de rezz possible
			if (q.isEmpty()) {
				
				//si la corp peut activer des cartes mais qu'il n'y a pas de question
				if (mayRezz)
					q.ask("none").to(() -> triggeredFlow.apply(null));
				else
					triggeredFlow.apply(null);
			}
			q.fire();
		}
	}

	public class RunnerDiscardEvent extends AbstractDiscardPhaseEvent {
	}

	public class RunnerReinitActionEvent extends AbstractReinitAction {
		RunnerReinitActionEvent() {
			super(runner, 4);
		}
	}

	public class RunnerStartOfTurnEvent extends AbstractTurnEvent {
	}

	public class RunnerTurnEndedEvent extends AbstractTurnEvent {
	}

	private static final Logger log = LoggerFactory.getLogger(Game.class);

	private final Corp corp;

	private final ConfigurableEventListener delegated;

	private final Flow end;

	private Optional<Notifier> notifier;

	private int qid = 0;

	private final Map<Integer, Question> questions = new HashMap<>();

	private WinCondition result;

	/**
	 * Le run
	 */
	private Run run;

	private final Runner runner;

	private GameStep step;

	private int turn = 0;

	public Game(Runner runner, Corp corp, ConfigurableEventListener delegated, Flow end) {
		this.runner = runner;
		this.corp = corp;
		this.delegated = delegated;
		this.end = end;
		setNotifier(null);
	}

	public Game(Runner runner, Corp corp, Flow end) {
		this(runner, corp, new ConfigurableEventListenerBasic(), end);
	}

	public WinCondition getResult() {
		return result;
	}

	@Override
	public <T extends Event> void apply(T event, Flow flow) {
		event.setGame(this);

		// on fait la vérification du flow
		delegated.apply(event, () -> {
			if (isEnded())
				end.apply();
			else
				flow.apply();
		});
	}

	private Set<Integer> asIntSet(List<? extends Card> hand) {
		Collector<Integer, ?, Set<Integer>> intSet = Collectors.toSet();
		Function<Card, Integer> getId = (Function<Card, Integer>) c -> c.getId();
		Set<Integer> among = hand.stream().collect(mapping(getId, intSet));
		return among;
	}

	/**
	 * Permet de poser une question
	 * 
	 * @param to
	 * @param what
	 * @return
	 */
	public Question ask(Player to, NotificationEvent what) {

		final int uid = qid++;
		// TODO faire mieux
		Question question = new Question(what, to, uid, this);
		questions.put(uid, question);
		return question;
	}

	public Object bind(EventMatcher<?> matcher) {
		return delegated.bind(matcher);
	}

	/**
	 * Le callback quand la corp d�fausse
	 * 
	 * @param discarded
	 */
	private void corpDiscardCardDone(Set<Integer> discarded) {

		Stream<CorpCard> s = corp.getHq().getCards().stream().filter(c -> discarded.contains(c.getId()));
		recurse(s.iterator(), (c, n) -> corp.discard(c, n), this::corpEndOfDiscardAction);
	}

	/**
	 * Phase corp 3.1
	 * 
	 * @param event
	 */
	private void corpDiscardStart(CorpDiscardEvent event) {

		setStep(GameStep.CORP_DISCARD);

		List<? extends Card> hand = corp.getHand();
		int hs = hand.size();
		int max = event.getMaxHandsize();

		if (hs > max) {
			int remove = hs - max;

			Question q = ask(Player.CORP, NotificationEvent.DISCARD_CARD);
			// TODO faire mieux avec gestion de la sélection
			q.ask("selected-card");

			// FIXME faire mieux
			// q.add("selected-card", remove, asIntSet(hand),
			// this::corpDiscardCardDone);
			q.fire();
		} else {
			corpEndOfDiscardAction();
		}
	}

	/**
	 * Phase corp 3.2, 3.3
	 */
	private void corpEndOfDiscardAction() {
		pingpong(() -> apply(new CorpTurnEndedEvent(), this::nextTurn)).corp();
	}

	/**
	 * Phase corp 2 et 2.1 et 2.2
	 * 
	 * @param event
	 */
	private void corpStartActionTurn(CorpReinitActionEvent event) {
		// mise en place des actions
		event.setupAction();

		setStep(GameStep.CORP_ACT);
		pingpong(() -> apply(new CorpDiscardEvent(), this::corpDiscardStart)).runner();
	}

	/**
	 * Phase corp 1.3
	 */
	private void corpTurnStarted() {
		corp.draw(() -> apply(new CorpReinitActionEvent(), this::corpStartActionTurn));
	}

	public Corp getCorp() {
		return corp;
	}

	public Map<Integer, Question> getQuestions() {
		return unmodifiableMap(questions);
	}

	public Run getRun() {
		return run;
	}

	public Runner getRunner() {
		return runner;
	}

	public GameStep getStep() {
		return step;
	}

	public boolean inStep(GameStep gs) {
		return gs == getStep();
	}

	public boolean isEnded() {
		return result != null;
	}

	/**
	 * Le tour suivant commence
	 */
	private void nextTurn() {

		log.debug("next turn {}", turn);

		if (turn++ % 2 == 0) {
			setStep(GameStep.CORP_DRAW);
			// corp phase 1.1, 1.2
			pingpong(() -> apply(new CorpStartOfTurnEvent(), this::corpTurnStarted)).corp();
		} else {
			setStep(GameStep.RUNNER_ACT);

			// runner phase 1.1
			pingpong(() -> apply(new RunnerReinitActionEvent(), this::runnerTurnStarted)).runner();
		}
	}

	@Override
	public void notification(Notification notif) {
		notifier.ifPresent((n) -> n.notification(notif));

	}

	/**
	 * Permet de créer un échange
	 * 
	 * @param next
	 * @return
	 */
	public PingPong pingpong(Flow next) {
		return new PingPong(next);
	}

	public void remove(Question q) {
		questions.remove(q.getQid());
	}

	/**
	 * Le callback quand le runner d�fausse
	 * 
	 * @param discarded
	 */
	private void runnerDiscardCardDone(Set<Integer> discarded) {

		Stream<CorpCard> s = corp.getHq().getCards().stream().filter(c -> discarded.contains(c.getId()));
		recurse(s.iterator(), (c, n) -> corp.discard(c, n), this::runnerEndOfDiscardAction);
	}

	/**
	 * Phase runner 2.1
	 * 
	 * @param event
	 */
	private void runnerDiscardStart(RunnerDiscardEvent event) {

		setStep(GameStep.RUNNER_DISCARD);

		List<? extends Card> hand = runner.getHand();
		int hs = hand.size();
		int max = event.getMaxHandsize();

		if (hs > max) {
			int remove = hs - max;

			// TODO faire mieux avec gestion de la sélection
			Question q = ask(Player.RUNNER, NotificationEvent.DISCARD_CARD);
			// q.add("selected-card", remove, asIntSet(hand),
			// this::runnerDiscardCardDone);
			q.fire();
		} else {
			runnerEndOfDiscardAction();
		}
	}

	/**
	 * Phase runner 2.2, 2.3
	 */
	private void runnerEndOfDiscardAction() {
		pingpong(() -> apply(new RunnerTurnEndedEvent(), this::nextTurn)).corp();
	}

	/**
	 * Phase runner 1.3
	 */
	private void runnerStartActionTurn() {
		pingpong(() -> apply(new RunnerDiscardEvent(), this::runnerDiscardStart)).corp();

	}

	/**
	 * Phase runner 1.2
	 * 
	 * @param event
	 */
	private void runnerTurnStarted(RunnerReinitActionEvent event) {

		// mise en place des actions
		event.setupAction();

		apply(new RunnerStartOfTurnEvent(), this::runnerStartActionTurn);
	}

	public void setNotifier(Notifier notifier) {
		this.notifier = Optional.ofNullable(notifier);
	}

	public void setResult(WinCondition result) {
		log.debug("WinCondition {}", result);
		this.result = result;
		notification(NotificationEvent.GAME_ENDED.apply().m(result));
	}

	private void setStep(GameStep step) {
		this.step = step;
		notification(NotificationEvent.NEXT_STEP.apply().m(step));
	}

	public Game setup() {
		corp.bind(this);
		runner.bind(this);
		corp.setGame(this);
		runner.setGame(this);

		int count = 0;
		for (Card rc : runner.getStack())
			rc.setId(count++).setGame(this);
		for (Card rc : corp.getStack())
			rc.setId(count++).setGame(this);
		return this;
	}

	public Game start() {
		apply(new GameStartedEvent(), this::nextTurn);
		return this;
	}

	/**
	 * Création d'un run
	 * 
	 * @param target
	 * @param next
	 */
	public void startRun(CorpServer target, Flow next) {
		setStep(GameStep.RUNNING);
		run = new Run(target, new FlowControler(this, () -> {
			setStep(GameStep.RUNNER_ACT);
			run = null;
			next.apply();
		}));
	}

	public void unbind(Object bindKey) {
		delegated.unbind(bindKey);
	}
}
