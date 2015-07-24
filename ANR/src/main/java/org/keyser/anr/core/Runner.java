package org.keyser.anr.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.keyser.anr.core.runner.Hardware;
import org.keyser.anr.core.runner.Program;
import org.keyser.anr.core.runner.Resource;

public class Runner extends AbstractId {

	private final AbstractCardContainer<Resource> resources = new AbstractCardContainer<>(CardLocation::resources);

	private final AbstractCardContainer<Program> programs = new AbstractCardContainer<>(CardLocation::programs);

	private final AbstractCardContainer<Hardware> hardwares = new AbstractCardContainer<>(CardLocation::hardwares);

	private final AbstractCardContainer<AbstractCardRunner> stack = new AbstractCardContainer<>(CardLocation::stack);

	private final AbstractCardContainer<AbstractCardRunner> grip = new AbstractCardContainer<>(CardLocation::grip);

	private final AbstractCardContainer<AbstractCardRunner> heap = new AbstractCardContainer<>(CardLocation::heap);

	private int link;

	protected Runner(int id, MetaCard meta) {
		super(id, meta, PlayerType.RUNNER, CardLocation::runnerScore);
	}

	@Override
	public PlayerType getOwner() {
		return PlayerType.RUNNER;
	}

	/**
	 * Permet de savoir si le runner est taggé
	 * 
	 * @return
	 */
	public boolean isTagged() {
		return hasAnyToken(TokenType.TAG);
	}

	/**
	 * Chargement de la configuration
	 * 
	 * @param def
	 * @param creator
	 */
	public void load(RunnerDef def, Function<AbstractTokenContainerId, AbstractCard> creator) {
		registerCard(def.getGrip(), a -> grip.add((AbstractCardRunner) a), creator);
		registerCard(def.getStack(), a -> stack.add((AbstractCardRunner) a), creator);
		registerCard(def.getHeap(), a -> heap.add((AbstractCardRunner) a), creator);
		registerCard(def.getHardwares(), a -> hardwares.add((Hardware) a), creator);
		registerCard(def.getPrograms(), a -> programs.add((Program) a), creator);
		registerCard(def.getResources(), a -> resources.add((Resource) a), creator);
	}

	/**
	 * Création de la définition du runner
	 * 
	 * @return
	 */
	public RunnerDef createRunnerDef() {
		RunnerDef def = new RunnerDef();
		updateIdDef(def);
		def.setResources(createDefList(resources));
		def.setPrograms(createDefList(programs));
		def.setHeap(createDefList(heap));
		def.setHardwares(createDefList(hardwares));
		def.setStack(createDefList(stack));
		def.setGrip(createDefList(grip));
		return def;
	}

	public AbstractCardContainer<Resource> getResources() {
		return resources;
	}

	public AbstractCardContainer<Program> getPrograms() {
		return programs;
	}

	public AbstractCardContainer<Hardware> getHardwares() {
		return hardwares;
	}

	public AbstractCardContainer<AbstractCardRunner> getStack() {
		return stack;
	}

	public AbstractCardContainer<AbstractCardRunner> getGrip() {
		return grip;
	}

	public AbstractCardContainer<AbstractCardRunner> getHeap() {
		return heap;
	}

	public void alterMemory(int delta, Flow next) {

		// TODO gestion de l'effet
		next.apply();
	}

	/**
	 * Gestion des dommages
	 * @param damage
	 * @param next
	 */
	public void doDamage(int damage, Flow next) {

		int size = grip.size();
		if (damage <= size) {
			
			AbstractCardList acl=cardsInHands();
			List<AbstractCard> cards = acl.getCards();
		
			//on prend les cartes au hasard
			Collections.shuffle(cards);			
			TrashList tl = new TrashList(TrashCause.DAMAGE);
			for (int i = 0; i < damage; i++)
				tl.add(cards.get(i));
			
			tl.trash(next);

		} else {
			//TODO  runner flatline !!
			next.apply();
		}

	}

	public void alterLink(int delta) {
		setLink(getLink() + delta);
	}

	public int getLink() {
		return link;
	}

	public void setLink(int link) {
		this.link = link;
		// TODO notification effect
	}

	@Override
	public void draw(int i, Flow next) {
		int size = Math.min(i, stack.size());

		List<AbstractCardRunner> cards = new ArrayList<>();
		for (int j = 0; j < size; j++)
			cards.add(stack.get(j));

		if (!cards.isEmpty())
			cards.stream().forEach(grip::add);
		next.apply();
	}

	@Override
	protected AbstractCardList cardsInHands() {
		AbstractCardList acl = new AbstractCardList();
		grip.stream().forEach(acl::add);
		return acl;
	}
}
