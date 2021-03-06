define([ "mix", "underscore", "anr/actionmodel", "./submodel", "util/observablemixin", "util/innersetmixin" ],//
function(mix, _, ActionModel, SubModel, ObservableMixin, InnerSetMixin) {

	function Action(bus, owner, defs) {

		this.bus = bus;
		this.owner = owner;
		this.actionType = defs.type || Action.DEFAULT_TYPE;

		// recopie de propriétés
		_.each(defs, function(val, key) {
			this[key] = val;
		}.bind(this));

		this.setVariableValue(0);

		// calcul de l'état
		this.observe(this.computeState.bind(this), [ Action.VARIABLE_VALUE, Action.ENABLED ]);
		this.enabled = true;
		this.selectedsSubs = null;
		this.selected = false;
		this.order = null;
		this.draggedValue = null;

		this.computeState();

		// enregistre l'action dans la carte
		if (_.isFunction(owner.registerAction)) {
			owner.registerAction(this);
		}
	}

	mix(Action, ObservableMixin);
	mix(Action, InnerSetMixin);
	mix(Action, function() {

		this.isEnabled = function() {
			return this.state && this.state.enabled;
		}

		/**
		 * Encodage de la réponse
		 */
		this.encodeResponse = function() {
			var response = { rid : this.id };
			if (this.isTraceAction())
				response.object = this.variableValue;
			else if (this.isBreakAction())
				response.object = _.map(this.selectedsSubs, function(s) {
					return s.id;
				});
			else if (this.isOrderingAction()) {
				response.object = this.order;
			} else if (this.isConfirmSelectionAction())
				response.object = _.map(this.selectedsAction, function(a) {
					return a.owner.id();
				});
			else if (this.isDragAction())
				response.object = this.draggedValue;

			return response;
		}

		/**
		 * Click sur l'action
		 */
		this.activate = function() {
			this.bus.activateAction(this);
		}

		/**
		 * Calcul l'état interne des couts
		 */
		this.computeState = function() {
			var nb = this.variableValue;
			if (this.isTraceAction()) {
				if (nb >= 0 && nb < this.max)
					this.setState({ enabled : this.enabled, cost : "{" + nb + ":credit}" });
			} else if (this.isBreakAction() || this.isConfirmSelectionAction()) {
				if (nb >= 0 && nb < this.costs.length) {
					var variable = this.costs[nb];
					this.setState({ enabled : variable.enabled == true, cost : variable.cost || '' });
				}
			} else {
				this.setState({ enabled : this.enabled, cost : this.cost });
			}
		}

		/**
		 * Renvoi vrai si l'action est une action de selection
		 */
		this.isSelectionAction = function() {
			return Action.SELECTION_TYPE === this.actionType;
		}

		/**
		 * Renvoi vrai si l'action est une action de confirmation de sélection
		 */
		this.isConfirmSelectionAction = function() {
			return Action.CONFIRM_SELECTION_TYPE === this.actionType;
		}

		/**
		 * Renvoi vrai si l'action est une action de trace
		 */
		this.isTraceAction = function() {
			return Action.TRACE_TYPE === this.actionType;
		}

		/**
		 * Renvoi vrai si l'action est une action de break
		 */
		this.isBreakAction = function() {
			return Action.BREAK_TYPE === this.actionType;
		}

		/**
		 * Renvoi vrai si l'action est une action de drag
		 */
		this.isDragAction = function() {
			return Action.DRAG_TYPE === this.actionType;
		}

		/**
		 * Renvoi si l'action est une action de drag, ou que le drag est activé
		 */
		this.isDragEnabled = function() {
			return this.isDragAction() || this.enableDrag;
		}

		/**
		 * Renvoi vrai si l'action est une action de réarrangement
		 */
		this.isOrderingAction = function() {
			return Action.ORDERING_TYPE === this.actionType;
		}

		/**
		 * Renvoi vrai si l'action est une action de no-op
		 */
		this.isNoopAction = function() {
			return Action.NOOP_TYPE === this.actionType;
		}

		/**
		 * Permet d'identifier les actions par défaut
		 */
		this.isDefaultAction = function() {
			return this.isNoopAction() || this.isConfirmSelectionAction();
		}

		this.setState = function(state) {
			this._innerSet(Action.STATE, state);
		}
		this.setText = function(text) {
			this._innerSet(Action.TEXT, text);
		}

		this.setEnabled = function(enabled) {
			this._innerSet(Action.ENABLED, enabled);
		}

		this.setVariableValue = function(value) {
			this._innerSet(Action.VARIABLE_VALUE, value);
		}

		this.setSelected = function(selected) {
			this._innerSet(Action.SELECTED, selected);
		}

		/**
		 * Permet de désactive le comportement de drag
		 */
		this.disableDrag = function() {
			this.owner.enableDrag(false);
		}
	});

	Action.STATE = "state";
	Action.TEXT = "text";
	Action.VARIABLE_VALUE = "variableValue";
	Action.SELECTED = "selected";
	Action.ENABLED = "enabled";

	Action.DEFAULT_TYPE = "default";
	Action.BREAK_TYPE = "break";
	Action.TRACE_TYPE = "trace";
	Action.DRAG_TYPE = "drag";
	Action.ORDERING_TYPE = "ordering";
	Action.SELECTION_TYPE = "selection";
	Action.CONFIRM_SELECTION_TYPE = "confirmselection";
	Action.NOOP_TYPE = "noop";

	function ActionBus(listener) {
		this.actions = [];
		this.lastActionId = -1;
		this.listener = listener;
	}

	mix(ActionBus, function() {

		/**
		 * Activation d'une action
		 */
		this.activateAction = function(action) {
			if (this.lastActionId < action.id) {

				if (action.isEnabled()) {

					var enc = action.encodeResponse();
					console.log("activateAction", action, "------------", enc);
					this.lastActionId = action.id;

					// nettoyage de la sélections
					_.each(this.actions, function(a) {
						if (a.isSelectionAction()) {
							a.setSelected(false);
						}

						if (a.isDragEnabled()) {
							a.disableDrag();
						}
					});

					// nettoyage des actions
					this.actions = [];

					if (this.listener)
						this.listener(enc);
				}
			}
		}

		/**
		 * Changement de l'ordre on le place dans les actions
		 */
		this.orderingChangeds = function(order) {
			_.each(this.actions, function(a) {
				if (a.isOrderingAction()) {
					a.order = order;
				}
			});
		}

		/**
		 * Attache un ecouteur dans tous les models de sub
		 */
		this.bindSubModel = function(submodel) {
			submodel.observe(function(evt) {

				var selecteds = evt.object.getSelecteds();
				_.each(this.actions, function(a) {
					if (a.isBreakAction()) {
						a.setVariableValue(selecteds.length);
						a.selectedsSubs = selecteds;
					}
				});

			}.bind(this), [ SubModel.SELECTED ]);

			submodel.observe(function(evt) {
				_.each(this.actions, function(a) {
					if (a.isBreakAction()) {
						a.setVariableValue(0);
						a.selectedsSubs = null;
					}
				});
			}.bind(this), [ SubModel.REMOVED ]);
		}

		/**
		 * Renvoi toutes les actions sélectionnées
		 */
		this.getSelectedsAction = function() {
			var selectedsActions = [];
			_.each(this.actions, function(a) {
				if (a.isSelectionAction() && a.selected)
					selectedsActions.push(a);
			});
			return selectedsActions;
		}

		/**
		 * Renvoi la premiere action par defaut
		 */
		this.getDefaultAction = function() {
			var selectedsActions = [];
			_.each(this.actions, function(a) {
				if (a.isDefaultAction())
					selectedsActions.push(a);
			});

			if (selectedsActions.length > 0)
				return selectedsActions[0];
			else
				return null;
		}

		/**
		 * Mise à jour de la confirmation
		 */
		this.updateConfirmSelection = function(evt) {
			var selecteds = this.getSelectedsAction();
			_.each(this.actions, function(a) {
				if (a.isConfirmSelectionAction()) {
					a.setVariableValue(selecteds.length);
					a.selectedsAction = selecteds;
				}
			});
		}

		/**
		 * Création des actions
		 */
		this.createActions = function(owner, actions) {
			return _.map(actions, function(def) {
				var act = new Action(this, owner, def);
				this.actions.push(act);
				if (act.isSelectionAction())
					act.observe(this.updateConfirmSelection.bind(this), [ Action.SELECTED ]);
				return act;
			}.bind(this));

		};
	});

	ActionBus.Action = Action;

	return ActionBus;
});