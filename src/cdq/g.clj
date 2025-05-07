(ns cdq.g
  (:require [cdq.audio.sound :as sound]
            [cdq.assets :as assets]
            [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera]
            [cdq.grid :as grid]
            [cdq.info :as info]
            [cdq.input :as input]
            [cdq.inventory :as inventory]
            [cdq.math.raycaster :as raycaster]
            [cdq.math.shapes :refer [circle->outer-rectangle]]
            [cdq.math.vector2 :as v]
            [cdq.tiled :as tiled]
            cdq.potential-fields
            [cdq.ui :as ui :refer [ui-actor]]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.stage :as stage]
            [cdq.ui.menu :as ui.menu]
            [cdq.utils :as utils :refer [readable-number
                                         pretty-pst
                                         sort-by-order
                                         define-order
                                         safe-merge
                                         tile->middle]]
            [cdq.val-max :as val-max]
            [cdq.world.content-grid :as content-grid]
            [clojure.data.grid2d :as g2d]
            [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [reduce-fsm :as fsm])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.scenes.scene2d Actor Group)
           (com.badlogic.gdx.scenes.scene2d.ui Image Widget)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable TextureRegionDrawable ClickListener)
           (com.badlogic.gdx.utils ScreenUtils)
           (com.badlogic.gdx.utils.viewport Viewport)))

(declare elapsed-time)

(defn ->timer [duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ elapsed-time duration)})

(defn stopped? [{:keys [stop-time]}]
  (>= elapsed-time stop-time))

(defn timer-reset [{:keys [duration] :as timer}]
  (assoc timer :stop-time (+ elapsed-time duration)))

(defn timer-ratio [{:keys [duration stop-time] :as timer}]
  {:post [(<= 0 % 1)]}
  (if (stopped? timer)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time elapsed-time) duration))))

(defn send-event!
  ([eid event]
   (send-event! eid event nil))
  ([eid event params]
   (when-let [fsm (:entity/fsm @eid)]
     (let [old-state-k (:state fsm)
           new-fsm (fsm/fsm-event fsm event)
           new-state-k (:state new-fsm)]
       (when-not (= old-state-k new-state-k)
         (let [old-state-obj (entity/state-obj @eid)
               new-state-obj [new-state-k (entity/create (if params
                                                           [new-state-k eid params]
                                                           [new-state-k eid]))]]
           (when (:entity/player? @eid)
             (when-let [cursor-key (state/cursor new-state-obj)]
               (graphics/set-cursor! cursor-key)))
           (swap! eid #(-> %
                           (assoc :entity/fsm new-fsm
                                  new-state-k (new-state-obj 1))
                           (dissoc old-state-k)))
           (state/exit!  old-state-obj)
           (state/enter! new-state-obj)))))))

; we cannot just set/unset movement direction
; because it is handled by the state enter/exit for npc/player movement state ...
; so we cannot expose it as a 'transaction'
; so the movement should be updated in the respective npc/player movement 'state' and no movement 'component' necessary !
; for projectiles inside projectile update !?
(defn set-movement [eid movement-vector]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

(defn mark-destroyed [eid]
  (swap! eid assoc :entity/destroyed? true))

(defn toggle-inventory-window []
  (ui/toggle-visible! (stage/get-inventory)))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (stage/get-actor ::modal)))
  (stage/add-actor (ui/window {:title title
                               :rows [[(ui/label text)]
                                      [(ui/text-button button-text
                                                       (fn []
                                                         (Actor/.remove (stage/get-actor ::modal))
                                                         (on-click)))]]
                               :id ::modal
                               :modal? true
                               :center-position [(/ (:width  graphics/ui-viewport) 2)
                                                 (* (:height graphics/ui-viewport) (/ 3 4))]
                               :pack? true})))

(defn add-skill [eid {:keys [property/id] :as skill}]
  {:pre [(not (entity/has-skill? @eid skill))]}
  (when (:entity/player? @eid)
    (action-bar/add-skill! (stage/get-action-bar) skill))
  (swap! eid assoc-in [:entity/skills id] skill))

(defn remove-skill [eid {:keys [property/id] :as skill}]
  {:pre [(entity/has-skill? @eid skill)]}
  (when (:entity/player? @eid)
    (action-bar/remove-skill! (stage/get-action-bar) skill))
  (swap! eid update :entity/skills dissoc id))

(defn- add-text-effect* [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter timer-reset))
           {:text text
            :counter (->timer 0.4)})))

(defn add-text-effect! [eid text]
  (swap! eid add-text-effect* text))

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta 0.04)

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  grid/Cell
  (blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied))

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- ->grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn- create-grid [tiled-map]
  (g2d/create-grid
   (tiled/tm-width tiled-map)
   (tiled/tm-height tiled-map)
   (fn [position]
     (atom (->grid-cell position
                        (case (tiled/movement-property tiled-map position)
                          "none" :none
                          "air"  :air
                          "all"  :all))))))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- create-raycaster [grid]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell grid/blocks-vision?))
    [arr width height]))

(declare tiled-map
         ^:private content-grid
         ^:private entity-ids
         explored-tile-corners
         grid
         raycaster
         potential-field-cache
         player-eid
         active-entities
         delta-time)

(def mouseover-eid nil)

(defn- set-cells! [grid eid]
  (let [cells (grid/rectangle->cells grid @eid)]
    (assert (not-any? nil? cells))
    (swap! eid assoc ::touched-cells cells)
    (doseq [cell cells]
      (assert (not (get (:entities @cell) eid)))
      (swap! cell update :entities conj eid))))

(defn- remove-from-cells! [eid]
  (doseq [cell (::touched-cells @eid)]
    (assert (get (:entities @cell) eid))
    (swap! cell update :entities disj eid)))

; could use inside tiles only for >1 tile bodies (for example size 4.5 use 4x4 tiles for occupied)
; => only now there are no >1 tile entities anyway
(defn- rectangle->occupied-cells [grid {:keys [left-bottom width height] :as rectangle}]
  (if (or (> (float width) 1) (> (float height) 1))
    (grid/rectangle->cells grid rectangle)
    [(grid [(int (+ (float (left-bottom 0)) (/ (float width) 2)))
            (int (+ (float (left-bottom 1)) (/ (float height) 2)))])]))

(defn- set-occupied-cells! [grid eid]
  (let [cells (rectangle->occupied-cells grid @eid)]
    (doseq [cell cells]
      (assert (not (get (:occupied @cell) eid)))
      (swap! cell update :occupied conj eid))
    (swap! eid assoc ::occupied-cells cells)))

(defn- remove-from-occupied-cells! [eid]
  (doseq [cell (::occupied-cells @eid)]
    (assert (get (:occupied @cell) eid))
    (swap! cell update :occupied disj eid)))

(defn- add-entity! [eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))

  (content-grid/update-entity! content-grid eid)

  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (set-cells! grid eid)
  (when (:collides? @eid)
    (set-occupied-cells! grid eid)))

(defn- remove-entity! [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))

  (content-grid/remove-entity! eid)

  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn position-changed! [eid]
  (content-grid/update-entity! content-grid eid)

  (remove-from-cells! eid)
  (set-cells! grid eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! grid eid)))

(defn remove-destroyed-entities! []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (remove-entity! eid)
    (doseq [component @eid]
      (entity/destroy! component eid))))

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-size 0.39) ; == spider smallest creature size.

(def z-orders [:z-order/on-ground
               :z-order/ground
               :z-order/flying
               :z-order/effect])

(def render-z-order (define-order z-orders))

(defrecord Body [position
                 left-bottom
                 width
                 height
                 half-width
                 half-height
                 radius
                 collides?
                 z-order
                 rotation-angle])

(defn- create-body [{[x y] :position
                     :keys [position
                            width
                            height
                            collides?
                            z-order
                            rotation-angle]}]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? minimum-size 0)))
  (assert (>= height (if collides? minimum-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set z-orders) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Body
   {:position (mapv float position)
    :left-bottom [(float (- x (/ width  2)))
                  (float (- y (/ height 2)))]
    :width  (float width)
    :height (float height)
    :half-width  (float (/ width  2))
    :half-height (float (/ height 2))
    :radius (float (max (/ width  2)
                        (/ height 2)))
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(defn- create-vs [components]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v])))
          {}
          components))

(def id-counter (atom 0))

(defn- spawn-entity [position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (safe-merge (-> components
                                      (assoc :entity/id (swap! id-counter inc))
                                      create-vs))))]
    (add-entity! eid)
    (doseq [component @eid]
      (entity/create! component eid))
    eid))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn spawn-audiovisual [position {:keys [tx/sound entity/animation]}]
  (sound/play! sound)
  (spawn-entity position
                effect-body-props
                {:entity/animation animation
                 :entity/delete-after-animation-stopped? true}))

; # :z-order/flying has no effect for now
; * entities with :z-order/flying are not flying over water,etc. (movement/air)
; because using potential-field for z-order/ground
; -> would have to add one more potential-field for each faction for z-order/flying
; * they would also (maybe) need a separate occupied-cells if they don't collide with other
; * they could also go over ground units and not collide with them
; ( a test showed then flying OVER player entity )
; -> so no flying units for now
(defn- ->body [{:keys [body/width body/height #_body/flying?]}]
  {:width  width
   :height height
   :collides? true
   :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)})

(defn spawn-creature [{:keys [position creature-id components]}]
  (let [props (db/build creature-id)]
    (spawn-entity position
                  (->body (:entity/body props))
                  (-> props
                      (dissoc :entity/body)
                      (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                      (safe-merge components)))))

(defn spawn-item [position item]
  (spawn-entity position
                {:width 0.75
                 :height 0.75
                 :z-order :z-order/on-ground}
                {:entity/image (:entity/image item)
                 :entity/item item
                 :entity/clickable {:type :clickable/item
                                    :text (:property/pretty-name item)}}))

(defn delayed-alert [position faction duration]
  (spawn-entity position
                effect-body-props
                {:entity/alert-friendlies-after-duration
                 {:counter (->timer duration)
                  :faction faction}}))

(defn line-render [{:keys [start end duration color thick?]}]
  (spawn-entity start
                effect-body-props
                #:entity {:line-render {:thick? thick? :end end :color color}
                          :delete-after-duration duration}))

(defn projectile-size [projectile]
  {:pre [(:entity/image projectile)]}
  (first (:world-unit-dimensions (:entity/image projectile))))

(defn spawn-projectile [{:keys [position direction faction]}
                        {:keys [entity/image
                                projectile/max-range
                                projectile/speed
                                entity-effects
                                projectile/piercing?] :as projectile}]
  (let [size (projectile-size projectile)]
    (spawn-entity position
                  {:width size
                   :height size
                   :z-order :z-order/flying
                   :rotation-angle (v/angle-from-vector direction)}
                  {:entity/movement {:direction direction
                                     :speed speed}
                   :entity/image image
                   :entity/faction faction
                   :entity/delete-after-duration (/ max-range speed)
                   :entity/destroy-audiovisual :audiovisuals/hit-wall
                   :entity/projectile-collision {:entity-effects entity-effects
                                                 :piercing? piercing?}})))

(def ^:private shout-radius 4)

(defn friendlies-in-radius [grid position faction]
  (->> {:position position
        :radius shout-radius}
       (grid/circle->entities grid)
       (filter #(= (:entity/faction @%) faction))))

(defn nearest-enemy [entity]
  (grid/nearest-entity @(grid (entity/tile entity))
                       (entity/enemy entity)))

(defn world-item? []
  (not (stage/mouse-on-actor?)))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.
(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn item-place-position [entity]
  (placement-point (:position entity)
                   (graphics/world-mouse-position)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn- spawn-enemies! []
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature (update props :position tile->middle))))

(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? true
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn- create-world-state! [{:keys [tiled-map start-position]}]
  (.bindRoot #'tiled-map tiled-map)
  (.bindRoot #'content-grid (content-grid/create {:cell-size 16
                                                  :width  (tiled/tm-width  tiled-map)
                                                  :height (tiled/tm-height tiled-map)}))
  (.bindRoot #'explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                            (tiled/tm-height tiled-map)
                                                            (constantly false))))
  (.bindRoot #'entity-ids (atom {}))
  (.bindRoot #'grid (create-grid tiled-map))
  (.bindRoot #'raycaster (create-raycaster grid))
  (.bindRoot #'potential-field-cache (atom nil))
  (spawn-enemies!)
  (.bindRoot #'player-eid (spawn-creature (player-entity-props start-position))))

(defn cache-active-entities!
  "Expensive operation.

  Active entities are those which are nearby the position of the player and about one screen away."
  []
  (.bindRoot #'active-entities (content-grid/active-entities content-grid @player-eid)))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [viewport entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (camera/position (:camera viewport))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (:width viewport))  2)))
     (<= ydist (inc (/ (float (:height viewport)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? graphics/world-viewport target))
       (not (and los-checks?
                 (raycaster/blocked? raycaster (:position source) (:position target))))))

(defn creatures-in-los-of-player []
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(line-of-sight? @player-eid @%))
       (remove #(:entity/player? @%))))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private inventory-cell-size 48)
(def ^:private inventory-droppable-color   [0   0.6 0 0.8])
(def ^:private inventory-not-allowed-color [0.6 0   0 0.8])

(defn- draw-inventory-cell-rect! [player-entity x y mouseover? cell]
  (graphics/rectangle x y inventory-cell-size inventory-cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (inventory/valid-slot? cell item)
                  inventory-droppable-color
                  inventory-not-allowed-color)]
      (graphics/filled-rectangle (inc x) (inc y) (- inventory-cell-size 2) (- inventory-cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-inventory-rect-actor []
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (let [^Actor this this]
        (draw-inventory-cell-rect! @player-eid
                                   (.getX this)
                                   (.getY this)
                                   (ui/hit this (graphics/mouse-position))
                                   (.getUserObject (.getParent this)))))))

(def ^:private slot->y-sprite-idx
  #:inventory.slot {:weapon   0
                    :shield   1
                    :rings    2
                    :necklace 3
                    :helm     4
                    :cloak    5
                    :chest    6
                    :leg      7
                    :glove    8
                    :boot     9
                    :bag      10}) ; transparent

(defn- slot->sprite-idx [slot]
  [21 (+ (slot->y-sprite-idx slot) 2)])

(defn- slot->sprite [slot]
  (graphics/from-sheet (graphics/sprite-sheet (assets/get "images/items.png") 48 48)
                       (slot->sprite-idx slot)))

(defn- slot->background [slot]
  (let [drawable (-> (slot->sprite slot)
                     :texture-region
                     ui/texture-region-drawable)]
    (BaseDrawable/.setMinSize drawable
                              (float inventory-cell-size)
                              (float inventory-cell-size))
    (TextureRegionDrawable/.tint drawable
                                 (Color. (float 1) (float 1) (float 1) (float 0.4)))))

(defn- ->inventory-cell [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        image-widget (ui/image-widget (slot->background slot)
                                      {:id :image})
        stack (ui/ui-stack [(draw-inventory-rect-actor)
                            image-widget])]
    (.setName stack "inventory-cell")
    (.setUserObject stack cell)
    (.addListener stack (proxy [ClickListener] []
                          (clicked [_event _x _y]
                            (entity/clicked-inventory-cell (entity/state-obj @player-eid)
                                                           cell))))
    stack))

(defn- inventory-table []
  (ui/table {:id ::table
             :rows (concat [[nil nil
                             (->inventory-cell :inventory.slot/helm)
                             (->inventory-cell :inventory.slot/necklace)]
                            [nil
                             (->inventory-cell :inventory.slot/weapon)
                             (->inventory-cell :inventory.slot/chest)
                             (->inventory-cell :inventory.slot/cloak)
                             (->inventory-cell :inventory.slot/shield)]
                            [nil nil
                             (->inventory-cell :inventory.slot/leg)]
                            [nil
                             (->inventory-cell :inventory.slot/glove)
                             (->inventory-cell :inventory.slot/rings :position [0 0])
                             (->inventory-cell :inventory.slot/rings :position [1 0])
                             (->inventory-cell :inventory.slot/boot)]]
                           (for [y (range (g2d/height (:inventory.slot/bag inventory/empty-inventory)))]
                             (for [x (range (g2d/width (:inventory.slot/bag inventory/empty-inventory)))]
                               (->inventory-cell :inventory.slot/bag :position [x y]))))}))

(defn- create-inventory-widget [position]
  (ui/window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position position
              :rows [[{:actor (inventory-table)
                       :pad 4}]]}))

(defn- inventory-cell-widget [cell]
  (get (::table (get (stage/get-actor :windows) :inventory-window)) cell))

(defn- set-item-image-in-widget [cell item]
  (let [cell-widget (inventory-cell-widget cell)
        image-widget (get cell-widget :image)
        drawable (ui/texture-region-drawable (:texture-region (:entity/image item)))]
    (BaseDrawable/.setMinSize drawable
                              (float inventory-cell-size)
                              (float inventory-cell-size))
    (Image/.setDrawable image-widget drawable)
    (ui/add-tooltip! cell-widget #(info/text item))))

(defn- remove-item-from-widget [cell]
  (let [cell-widget (inventory-cell-widget cell)
        image-widget (get cell-widget :image)]
    (Image/.setDrawable image-widget (slot->background (cell 0)))
    (ui/remove-tooltip! cell-widget)))

(defn set-item [eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (when (:entity/player? entity)
      (set-item-image-in-widget cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))))

(defn remove-item [eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      (remove-item-from-widget cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [eid cell]
  (let [item (get-in (:entity/inventory @eid) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (remove-item! eid cell)
       (set-item! eid cell (update item :count dec)))
      (remove-item! eid cell))))

; TODO no items which stack are available
(defn stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item eid cell)
            (set-item eid cell (update cell-item :count + (:count item))))))

(defn pickup-item [eid item]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (stack-item eid cell item)
      (set-item eid cell item))))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn- ->label-text []
  ; items then have 2x pretty-name
  #_(.setText (.getTitleLabel window)
              (if-let [eid mouseover-eid]
                (info/text [:property/pretty-name (:property/pretty-name @eid)])
                "Entity Info"))
  (when-let [eid mouseover-eid]
    (info/text ; don't use select-keys as it loses Entity record type
               (apply dissoc @eid disallowed-keys))))

(defn- entity-info-window [position]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position position
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.addActor window (ui-actor {:act (fn []
                                        (.setText label (str (->label-text)))
                                        (.pack window))}))
    window))

(defn- render-infostr-on-bar [infostr x y h]
  (graphics/draw-text {:text infostr
                       :x (+ x 75)
                       :y (+ y 2)
                       :up? true}))

(defn- hp-mana-bar [[x y-mana]]
  (let [rahmen      (graphics/->sprite (assets/get "images/rahmen.png"))
        hpcontent   (graphics/->sprite (assets/get "images/hp.png"))
        manacontent (graphics/->sprite (assets/get "images/mana.png"))
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (graphics/draw-image rahmen [x y])
                            (graphics/draw-image (graphics/sub-sprite contentimage [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                                 [x y])
                            (render-infostr-on-bar (str (utils/readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (entity/mana      player-entity) "MP")))})))

(defn- draw-player-message []
  (when-let [text (:text @stage/player-message)]
    (graphics/draw-text {:x (/ (:width     graphics/ui-viewport) 2)
                         :y (+ (/ (:height graphics/ui-viewport) 2) 200)
                         :text text
                         :scale 2.5
                         :up? true})))

(defn- check-remove-message []
  (when (:text @stage/player-message)
    (swap! stage/player-message update :counter + (.getDeltaTime Gdx/graphics))
    (when (>= (:counter @stage/player-message)
              (:duration-seconds @stage/player-message))
      (swap! stage/player-message dissoc :counter :text))))

(defn- player-message-actor []
  (ui-actor {:draw draw-player-message
             :act  check-remove-message}))

(defn- player-state-actor []
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @player-eid))}))

(declare dev-menu-config)

(defn- reset-game! [world-fn]
  (stage/init-state!)
  (stage/clear!)
  (run! stage/add-actor [(ui.menu/create (dev-menu-config))
                         (action-bar/create)
                         (hp-mana-bar [(/ (:width graphics/ui-viewport) 2)
                                       80 ; action-bar-icon-size
                                       ])
                         (ui/group {:id :windows
                                    :actors [(entity-info-window [(:width graphics/ui-viewport) 0])
                                             (create-inventory-widget [(:width  graphics/ui-viewport)
                                                                       (:height graphics/ui-viewport)])]})
                         (player-state-actor)
                         (player-message-actor)])
  (.bindRoot #'elapsed-time 0)
  (create-world-state! ((requiring-resolve world-fn) (db/build-all :properties/creatures))))

(declare paused?)

;"Mouseover-Actor: "
#_(when-let [actor (stage/mouse-on-actor? context)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn- dev-menu-config []
  {:menus [{:label "World"
            :items (for [world-fn '[cdq.level.vampire/create
                                    cdq.level.uf-caves/create
                                    cdq.level.modules/create]]
                     {:label (str "Start " (namespace world-fn))
                      :on-click (fn [] (reset-game! world-fn))})}
           {:label "Help"
            :items [{:label "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}]}
           {:label "Objects"
            :items (for [property-type (sort (filter #(= "properties" (namespace %))
                                                     (keys @#'db/-schemas)))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn []
                                  ((requiring-resolve 'cdq.editor/open-main-window!) property-type))})}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn []
                                 (when-let [entity (and mouseover-eid @mouseover-eid)]
                                   (:entity/id entity)))
                    :icon (assets/get "images/mouseover.png")}
                   {:label "elapsed-time"
                    :update-fn (fn [] (str (readable-number elapsed-time) " seconds"))
                    :icon (assets/get "images/clock.png")}
                   {:label "paused?"
                    :update-fn (fn [] paused?)}
                   {:label "GUI"
                    :update-fn (fn [] (graphics/mouse-position))}
                   {:label "World"
                    :update-fn (fn [] (mapv int (graphics/world-mouse-position)))}
                   {:label "Zoom"
                    :update-fn (fn [] (camera/zoom (:camera graphics/world-viewport)))
                    :icon (assets/get "images/zoom.png")}
                   {:label "FPS"
                    :update-fn (fn [] (.getFramesPerSecond Gdx/graphics))
                    :icon (assets/get "images/fps.png")}]})

(def ^:private explored-tile-color (Color. (float 0.5) (float 0.5) (float 0.5) (float 1)))

(def ^:private ^:dbg-flag see-all-tiles? false)

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- tile-color-setter [raycaster explored-tile-corners light-position]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored? explored-tile-color Color/BLACK)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (raycaster/blocked? raycaster light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles? Color/WHITE base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              Color/WHITE))))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(def ^:private factions-iterations {:good 15 :evil 5})

(defn- draw-before-entities! []
  (let [cam (:camera graphics/world-viewport)
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when tile-grid?
      (graphics/grid (int left-x) (int bottom-y)
                     (inc (int (:width  graphics/world-viewport)))
                     (+ 2 (int (:height graphics/world-viewport)))
                     1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell (grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (graphics/filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (graphics/filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (graphics/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- geom-test []
  (let [position (graphics/world-mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (graphics/circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
      (graphics/rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (graphics/rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile []
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (graphics/world-mouse-position))
          cell (grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (graphics/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5]))))))

(defn- draw-after-entities! []
  #_(geom-test)
  (highlight-mouseover-tile))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (graphics/rectangle x y (:width entity) (:height entity) color)))

; I can create this later after loading all the component namespaces
; just go through the systems
; and see which components are signed up for it
; => I get an overview what is rendered how...
#_(def ^:private entity-render-fns
  {:below {:entity/mouseover? draw-faction-ellipse
           :player-item-on-cursor draw-world-item-if-exists
           :stunned draw-stunned-circle}
   :default {:entity/image draw-image-as-of-body
             :entity/clickable draw-text-when-mouseover-and-text
             :entity/line-render draw-line}
   :above {:npc-sleeping draw-zzzz
           :entity/string-effect draw-text
           :entity/temp-modifier draw-filled-circle-grey}
   :info {:entity/hp draw-hpbar-when-mouseover-and-not-full
          :active-skill draw-skill-image-and-active-effect}})

(defn- render-entities! []
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            render! [entity/render-below!
                     entity/render-default!
                     entity/render-above!
                     entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect entity (if (:collides? entity) :white :gray)))
       (doseq [component entity]
         (render! component entity))
       (catch Throwable t
         (draw-body-rect entity :red)
         (pretty-pst t))))))

(defn- update-mouseover-entity! []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities grid (graphics/world-mouse-position)))]
                    (->> render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(line-of-sight? player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (.bindRoot #'mouseover-eid new-eid)))

(def pausing? true)

(defn- set-paused-flag! []
  (.bindRoot #'paused? (or #_error
                           (and pausing?
                                (state/pause-game? (entity/state-obj @player-eid))
                                (not (or (input/key-just-pressed? :p)
                                         (input/key-pressed?      :space)))))))

(defn- update-time! []
  (let [delta-ms (min (.getDeltaTime Gdx/graphics) max-delta)]
    (alter-var-root #'elapsed-time + delta-ms)
    (.bindRoot #'delta-time delta-ms)))

(defn- update-potential-fields! []
  (doseq [[faction max-iterations] factions-iterations]
    (cdq.potential-fields/tick potential-field-cache
                               grid
                               faction
                               active-entities
                               max-iterations)))

(defn- tick-entities! []
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (entity/tick! [k v] eid))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (stage/error-window! t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- camera-controls! []
  (let [camera (:camera graphics/world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed)))))

(defn- window-controls! []
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? (get window-hotkeys window-id))]
      (ui/toggle-visible! (get (stage/get-actor :windows) window-id))))
  (when (input/key-just-pressed? :escape)
    (let [windows (Group/.getChildren (stage/get-actor :windows))]
      (when (some Actor/.isVisible windows)
        (run! #(Actor/.setVisible % false) windows)))))

; 'cdq.g' -> start with '-main' -> has the API for all state stuff
; and nothing else
; -> all logic into sub-namespaces

; 1. cdq.db
; -> used by editor! -> stage actors dynamically add ?
; -> editor part of game ?

; 2. cdq.assets
; -> cdq.audio.sound
;   -> editor

; 3. cdq.graphics
; -> db, editor, stage

; 4. cdq.ui.stage
; -> editor

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)]
    (doseq [ns-sym (:requires config)]
      (require ns-sym))
    (db/create!)
    (lwjgl/application! (:application config)
                        (proxy [ApplicationAdapter] []
                          (create []
                            (assets/create! (:assets config))
                            (graphics/create! (:graphics config))
                            (ui/load! (:vis-ui config)
                                       ; we have to pass batch as we use our draw-image/shapes with our other batch inside stage actors
                                      ; -> tests ?, otherwise could use custom batch also from stage itself and not depend on 'graphics', also pass ui-viewport and dont put in graphics
                                      ) ; TODO we don't do dispose! ....
                            (stage/init!)
                            (reset-game! (:world-fn config)))

                          (dispose []
                            (assets/dispose!)
                            (graphics/dispose!)
                            ; TODO dispose tiled-map !! also @ reset-game ?!
                            )

                          (render []
                            (cache-active-entities!)
                            (graphics/set-camera-position! (:position @player-eid))
                            (ScreenUtils/clear Color/BLACK)
                            (graphics/draw-tiled-map tiled-map
                                                     (tile-color-setter raycaster
                                                                        explored-tile-corners
                                                                        (camera/position (:camera graphics/world-viewport))))
                            (graphics/draw-on-world-view! (fn []
                                                            (draw-before-entities!)
                                                            (render-entities!)
                                                            (draw-after-entities!)))
                            (stage/draw!)
                            (stage/act!)
                            (entity/manual-tick (entity/state-obj @player-eid))
                            (update-mouseover-entity!)
                            (set-paused-flag!)
                            (when-not paused?
                              (update-time!)
                              (update-potential-fields!)
                              (tick-entities!))

                            ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                            (remove-destroyed-entities!)

                            (camera-controls!)
                            (window-controls!))

                          (resize [width height]
                            (Viewport/.update graphics/ui-viewport    width height true)
                            (Viewport/.update graphics/world-viewport width height false))))))
