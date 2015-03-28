define([ "mix", "underscore", "util/observablemixin","util/innersetmixin", "geometry/rectangle" ],
		function(mix, _, ObservableMixin,InnerSetMixin, Rectangle) {
	function AbstractBox(layoutManager) {
		this.layoutManager = layoutManager;
		this._boxId = layoutManager.createId();
		this.local = new Rectangle();
		this.screen = new Rectangle();
		/**
		 * Le parent direct
		 */
		this.container = null;

		/**
		 * La profondeur
		 */
		this.depth = 0;

		/**
		 * Rotation
		 */
		this.rotation = 0.0;

		/**
		 * ZIndex
		 */
		this.zIndex = 0;

		/**
		 * Visibilité
		 */
		this.visible = true;

		// un changement sur le local provoque un needToMergetoScreen
		this.local.observe(this.needMergeToScreen.bind(this), [ Rectangle.MOVE_TO, Rectangle.RESIZE_TO ]);

	}
	// constante
	AbstractBox.DEPTH = "depth";
	AbstractBox.CONTAINER = "container";
	AbstractBox.ROTATION = "rotation";
	AbstractBox.ZINDEX = "zIndex";
	AbstractBox.VISIBLE = "visible";

	// application du mixin
	mix(AbstractBox, ObservableMixin);
	mix(AbstractBox, InnerSetMixin);	
	mix(AbstractBox, function() {

		/**
		 * Indique le besoin de recopie les coordonnées local dans les
		 * coordonnées screen
		 */
		this.needMergeToScreen = function() {
			this.layoutManager.needMergeToScreen(this);
		}

		/**
		 * Permet de modifier la position depuis le container parent.
		 * 
		 * moveTo est entree/sortie
		 */
		this.mergePosition = function(moveTo) {
			if (this.container != null) {
				var topLeft = this.container.screen.topLeft()
				moveTo.add(topLeft);
			}

			if (_.isFunction(this.additionnalMergePosition)) {
				this.additionnalMergePosition(moveTo);
			}
		}

		/**
		 * Recopie les coordonnées de l'élement local+l'élément screen du parent
		 * dans l'élément screen
		 */
		this.mergeToScreen = function() {
			var moveTo = this.local.topLeft();
			var sizeTo = this.local.size;
			this.mergePosition(moveTo);
			this.screen.moveTo(moveTo);
			this.screen.resizeTo(sizeTo);
		}


		/**
		 * Changement d'angle
		 */
		this.setRotation = function(rotation) {
			this._innerSet(AbstractBox.ROTATION, rotation);
		}

		/**
		 * Changement de profondeur
		 */
		this.setZIndex = function(zIndex) {
			this._innerSet(AbstractBox.ZINDEX, zIndex);
		}

		/**
		 * Changement de visibilite
		 */
		this.setVisible = function(visible) {
			this._innerSet(AbstractBox.VISIBLE, visible);
		}

		/**
		 * Modifie la profondeur du composant
		 */
		this.setDepth = function(depth) {
			this._innerSet(AbstractBox.DEPTH, depth);
		}

		/**
		 * Place le parent
		 */
		this.setContainer = function(container) {

			var oldContainer = this.container;
			if (oldContainer)
				oldContainer.removeChild(this);

			this._innerSet(AbstractBox.CONTAINER, container);
		}
	});

	return AbstractBox;
});