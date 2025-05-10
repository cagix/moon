(ns cdq.g
  (:require [cdq.data.val-max :as val-max]
            [cdq.db :as db]
            [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.g.assets]
            [cdq.g.db]
            [cdq.g.graphics]
            [cdq.g.world]
            [cdq.graphics :as graphics]
            [cdq.info :as info]
            [cdq.ui.action-bar :as action-bar]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            cdq.world.potential-fields
            [clojure.data.animation :as animation]
            [clojure.data.grid2d :as g2d]
            [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.interop :as interop]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.scene2d.ui :as ui :refer [ui-actor]]
            [clojure.gdx.scene2d.ui.menu :as ui.menu]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.math :refer [circle->outer-rectangle]]
            [clojure.gdx.math.raycaster :as raycaster]
            [clojure.gdx.math.vector2 :as v]
            [clojure.gdx.utils.disposable :refer [dispose!]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.utils :as utils :refer [readable-number
                                             sort-by-order
                                             define-order
                                             safe-merge
                                             tile->middle
                                             pretty-pst
                                             with-err-str]]
            [reduce-fsm :as fsm])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.scenes.scene2d Actor Group)
           (com.badlogic.gdx.scenes.scene2d.ui Image Widget)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable TextureRegionDrawable ClickListener)))

(declare ^:private stage)

(declare ^:private player-message)

(defn get-actor [id-keyword]
  (id-keyword stage))

(defn show-player-msg! [text]
  (swap! player-message assoc :text text :counter 0))

(defn mouse-on-actor? []
  (stage/hit stage (graphics/mouse-position ctx/graphics)))

(defn add-actor [actor]
  (stage/add-actor! stage actor))

(defn get-inventory []
  (get (:windows stage) :inventory-window))

(defn get-action-bar []
  (action-bar/get-data stage))

(defn selected-skill []
  (action-bar/selected-skill (get-action-bar)))

(defn error-window! [throwable]
  (pretty-pst throwable)
  (add-actor (ui/window {:title "Error"
                         :rows [[(ui/label (binding [*print-level* 3]
                                             (with-err-str
                                               (clojure.repl/pst throwable))))]]
                         :modal? true
                         :close-button? true
                         :close-on-escape? true
                         :center? true
                         :pack? true})))

(defn play-sound! [sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       ctx/assets
       sound/play!))

(defn ->timer [duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ (:elapsed-time ctx/world) duration)})

(defn stopped? [{:keys [stop-time]}]
  (>= (:elapsed-time ctx/world) stop-time))

(defn timer-reset [{:keys [duration] :as timer}]
  (assoc timer :stop-time (+ (:elapsed-time ctx/world) duration)))

(defn timer-ratio [{:keys [duration stop-time] :as timer}]
  {:post [(<= 0 % 1)]}
  (if (stopped? timer)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time (:elapsed-time ctx/world)) duration))))

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
               (graphics/set-cursor! ctx/graphics cursor-key)))
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
  (ui/toggle-visible! (get-inventory)))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (get-actor ::modal)))
  (add-actor (ui/window {:title title
                         :rows [[(ui/label text)]
                                [(ui/text-button button-text
                                                 (fn []
                                                   (Actor/.remove (get-actor ::modal))
                                                   (on-click)))]]
                         :id ::modal
                         :modal? true
                         :center-position [(/ (:width  (:ui-viewport ctx/graphics)) 2)
                                           (* (:height (:ui-viewport ctx/graphics)) (/ 3 4))]
                         :pack? true})))

(defn add-skill [eid {:keys [property/id] :as skill}]
  {:pre [(not (entity/has-skill? @eid skill))]}
  (when (:entity/player? @eid)
    (action-bar/add-skill! (get-action-bar) skill))
  (swap! eid assoc-in [:entity/skills id] skill))

(defn remove-skill [eid {:keys [property/id] :as skill}]
  {:pre [(entity/has-skill? @eid skill)]}
  (when (:entity/player? @eid)
    (action-bar/remove-skill! (get-action-bar) skill))
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

(defn- add-entity! [{:keys [entity-ids content-grid grid]} eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))

  (content-grid/update-entity! content-grid eid)

  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (set-cells! grid eid)
  (when (:collides? @eid)
    (set-occupied-cells! grid eid)))

(defn- remove-entity! [{:keys [entity-ids content-grid grid]} eid]
  (let [id (:entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))

  (content-grid/remove-entity! eid)

  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn position-changed! [{:keys [content-grid grid]} eid]
  (content-grid/update-entity! content-grid eid)

  (remove-from-cells! eid)
  (set-cells! grid eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! grid eid)))

(defn- remove-destroyed-entities! [{:keys [entity-ids] :as world}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (remove-entity! world eid)
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
    (add-entity! ctx/world eid)
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
  (play-sound! sound)
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
  (let [props (db/build ctx/db creature-id)]
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

(defn nearest-enemy [{:keys [grid]} entity]
  (grid/nearest-entity @(grid (entity/tile entity))
                       (entity/enemy entity)))

(defn world-item? []
  (not (mouse-on-actor?)))

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
                   (graphics/world-mouse-position ctx/graphics)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn- spawn-enemies! []
  (doseq [props (for [[position creature-id] (tiled/positions-with-property (:tiled-map ctx/world) :creatures :id)]
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

(defn- cache-active-entities
  "Expensive operation.

  Active entities are those which are nearby the position of the player and about one screen away."
  [{:keys [content-grid player-eid] :as world}]
  (assoc world :active-entities (content-grid/active-entities content-grid @player-eid)))

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
           (on-screen? (:world-viewport ctx/graphics) target))
       (not (and los-checks?
                 (raycaster/blocked? (:raycaster ctx/world) (:position source) (:position target))))))

(defn creatures-in-los-of-player [{:keys [active-entities player-eid]}]
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

(defn- draw-inventory-cell-rect! [g player-entity x y mouseover? cell]
  (graphics/draw-rectangle g x y inventory-cell-size inventory-cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (inventory/valid-slot? cell item)
                  inventory-droppable-color
                  inventory-not-allowed-color)]
      (graphics/draw-filled-rectangle g (inc x) (inc y) (- inventory-cell-size 2) (- inventory-cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-inventory-rect-actor []
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (let [^Actor this this
            g ctx/graphics]
        (draw-inventory-cell-rect! g
                                   @(:player-eid ctx/world)
                                   (.getX this)
                                   (.getY this)
                                   (ui/hit this (graphics/mouse-position g))
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
  (graphics/from-sheet ctx/graphics
                       (graphics/sprite-sheet ctx/graphics
                                              (ctx/assets "images/items.png")
                                              48 48)
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
                            (state/clicked-inventory-cell (entity/state-obj @(:player-eid ctx/world))
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
  (get (::table (get (get-actor :windows) :inventory-window)) cell))

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
              (if-let [eid (:mouseover-eid ctx/world)]
                (info/text [:property/pretty-name (:property/pretty-name @eid)])
                "Entity Info"))
  (when-let [eid (:mouseover-eid ctx/world)]
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

(defn- render-infostr-on-bar [g infostr x y h]
  (graphics/draw-text g {:text infostr
                         :x (+ x 75)
                         :y (+ y 2)
                         :up? true}))

(defn- hp-mana-bar [[x y-mana]]
  (let [rahmen      (graphics/sprite ctx/graphics (ctx/assets "images/rahmen.png"))
        hpcontent   (graphics/sprite ctx/graphics (ctx/assets "images/hp.png"))
        manacontent (graphics/sprite ctx/graphics (ctx/assets "images/mana.png"))
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [g x y contentimage minmaxval name]
                            (graphics/draw-image g rahmen [x y])
                            (graphics/draw-image g (graphics/sub-sprite g
                                                                        contentimage
                                                                        [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                                 [x y])
                            (render-infostr-on-bar g (str (utils/readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @(:player-eid ctx/world)
                             x (- x (/ rahmenw 2))
                             g ctx/graphics]
                         (render-hpmana-bar g x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                         (render-hpmana-bar g x y-mana manacontent (entity/mana      player-entity) "MP")))})))

(defn- draw-player-message [g]
  (when-let [text (:text @player-message)]
    (graphics/draw-text g {:x (/ (:width     (:ui-viewport g)) 2)
                           :y (+ (/ (:height (:ui-viewport g)) 2) 200)
                           :text text
                           :scale 2.5
                           :up? true})))

(defn- check-remove-message []
  (when (:text @player-message)
    (swap! player-message update :counter + (gdx/delta-time))
    (when (>= (:counter @player-message)
              (:duration-seconds @player-message))
      (swap! player-message dissoc :counter :text))))

(defn- player-message-actor []
  (ui-actor {:draw (fn [] (draw-player-message ctx/graphics))
             :act  check-remove-message}))

(defn- player-state-actor []
  (ui-actor {:draw #(state/draw-gui-view (entity/state-obj @(:player-eid ctx/world)))}))

(declare dev-menu-config)

(defn- reset-game! [world-fn]
  (.bindRoot #'player-message (atom {:duration-seconds 1.5}))
  (stage/clear! stage)
  (run! add-actor [(ui.menu/create (dev-menu-config))
                   (action-bar/create)
                   (hp-mana-bar [(/ (:width (:ui-viewport ctx/graphics)) 2)
                                 80 ; action-bar-icon-size
                                 ])
                   (ui/group {:id :windows
                              :actors [(entity-info-window [(:width (:ui-viewport ctx/graphics)) 0])
                                       (create-inventory-widget [(:width  (:ui-viewport ctx/graphics))
                                                                 (:height (:ui-viewport ctx/graphics))])]})
                   (player-state-actor)
                   (player-message-actor)])
  (.bindRoot #'ctx/world (cdq.g.world/create ((requiring-resolve world-fn)
                                              (db/build-all ctx/db :properties/creatures))))
  (spawn-enemies!)
  (alter-var-root #'ctx/world assoc :player-eid (spawn-creature (player-entity-props (:start-position ctx/world)))))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor? context)]
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
                                                     (keys (:schemas ctx/db))))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn []
                                  ((requiring-resolve 'cdq.ui.editor/open-main-window!) property-type))})}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn []
                                 (when-let [entity (and (:mouseover-eid ctx/world)
                                                        @(:mouseover-eid ctx/world))]
                                   (:entity/id entity)))
                    :icon (ctx/assets "images/mouseover.png")}
                   {:label "elapsed-time"
                    :update-fn (fn [] (str (readable-number (:elapsed-time ctx/world)) " seconds"))
                    :icon (ctx/assets "images/clock.png")}
                   {:label "paused?"
                    :update-fn (fn [] (:paused? ctx/world))}
                   {:label "GUI"
                    :update-fn (fn [] (graphics/mouse-position ctx/graphics))}
                   {:label "World"
                    :update-fn (fn [] (mapv int (graphics/world-mouse-position ctx/graphics)))}
                   {:label "Zoom"
                    :update-fn (fn [] (camera/zoom (:camera (:world-viewport ctx/graphics))))
                    :icon (ctx/assets "images/zoom.png")}
                   {:label "FPS"
                    :update-fn (fn [] (gdx/frames-per-second))
                    :icon (ctx/assets "images/fps.png")}]})

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
  (let [g ctx/graphics
        cam (:camera (:world-viewport g))
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when tile-grid?
      (graphics/draw-grid g
                          (int left-x) (int bottom-y)
                          (inc (int (:width  (:world-viewport g))))
                          (+ 2 (int (:height (:world-viewport g))))
                          1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell ((:grid ctx/world) [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (graphics/draw-filled-rectangle g x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (graphics/draw-filled-rectangle g x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (graphics/draw-filled-rectangle g x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- geom-test [g]
  (let [position (graphics/world-mouse-position g)
        radius 0.8
        circle {:position position :radius radius}]
    (graphics/draw-circle g position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells (:grid ctx/world) circle))]
      (graphics/draw-rectangle g x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (graphics/draw-rectangle g x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile [g]
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (graphics/world-mouse-position g))
          cell ((:grid ctx/world) [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (graphics/draw-rectangle g x y 1 1
                                 (case (:movement @cell)
                                   :air  [1 1 0 0.5]
                                   :none [1 0 0 0.5]))))))

(defn- draw-after-entities! []
  #_(geom-test ctx/graphics)
  (highlight-mouseover-tile ctx/graphics))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [g entity color]
  (let [[x y] (:left-bottom entity)]
    (graphics/draw-rectangle g x y (:width entity) (:height entity) color)))

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
  (let [entities (map deref (:active-entities ctx/world))
        player @(:player-eid ctx/world)
        g ctx/graphics]
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
         (draw-body-rect g entity (if (:collides? entity) :white :gray)))
       (doseq [component entity]
         (render! component entity g))
       (catch Throwable t
         (draw-body-rect g entity :red)
         (pretty-pst t))))))

(defn- update-mouseover-entity! []
  (let [new-eid (if (mouse-on-actor?)
                  nil
                  (let [player @(:player-eid ctx/world)
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities (:grid ctx/world)
                                                           (graphics/world-mouse-position ctx/graphics)))]
                    (->> render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(line-of-sight? player @%))
                         first)))]
    (when-let [eid (:mouseover-eid ctx/world)]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (alter-var-root #'ctx/world assoc :mouseover-eid new-eid)))

(def pausing? true)

(defn- pause-game? []
  (or #_error
      (and pausing?
           (state/pause-game? (entity/state-obj @(:player-eid ctx/world)))
           (not (or (gdx/key-just-pressed? :p)
                    (gdx/key-pressed?      :space))))))

; TODO here timers check stopped? ???
(defn- update-time [world graphics-delta]
  (let [delta-ms (min graphics-delta max-delta)]
    (-> world
        (update :elapsed-time + delta-ms)
        (assoc :delta-time delta-ms))))

(defn- update-potential-fields! [{:keys [potential-field-cache
                                         grid
                                         active-entities]}]
  (doseq [[faction max-iterations] factions-iterations]
    (cdq.world.potential-fields/tick potential-field-cache
                                     grid
                                     faction
                                     active-entities
                                     max-iterations)))

(defn- tick-entities! [{:keys [active-entities]}]
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
     (error-window! t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- camera-controls! []
  (let [camera (:camera (:world-viewport ctx/graphics))
        zoom-speed 0.025]
    (when (gdx/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (gdx/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed)))))

(defn- window-controls! []
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (gdx/key-just-pressed? (get window-hotkeys window-id))]
      (ui/toggle-visible! (get (get-actor :windows) window-id))))
  (when (gdx/key-just-pressed? :escape)
    (let [windows (Group/.getChildren (get-actor :windows))]
      (when (some Actor/.isVisible windows)
        (run! #(Actor/.setVisible % false) windows)))))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)]
    (doseq [ns-sym (:requires config)]
      (require ns-sym))
    (.bindRoot #'ctx/db (cdq.g.db/create))
    (lwjgl/application! (:application config)
                        (proxy [ApplicationAdapter] []
                          (create []
                            ; TODO Input / App / Files / Graphics from Gdx ?
                            ; TODO can bind cdq.g.graphics or asssets dynamically ...
                            (.bindRoot #'ctx/assets   (cdq.g.assets/create (:assets config)))
                            (.bindRoot #'ctx/graphics (cdq.g.graphics/create (:graphics config)))
                            (ui/load! (:vis-ui config)) ; TODO we don't do dispose!
                            (.bindRoot #'stage (stage/create (:ui-viewport ctx/graphics)
                                                             (:batch       ctx/graphics)))
                            (gdx/set-input-processor! stage)
                            (reset-game! (:world-fn config)))

                          (dispose []
                            (dispose! ctx/assets)
                            (dispose! ctx/graphics)
                            ; TODO dispose world tiled-map/level resources?
                            )

                          (render []
                            (alter-var-root #'ctx/world cache-active-entities)
                            (graphics/set-camera-position! ctx/graphics (:position @(:player-eid ctx/world)))
                            (graphics/clear-screen! ctx/graphics)
                            (graphics/draw-tiled-map ctx/graphics
                                                     (:tiled-map ctx/world)
                                                     (tile-color-setter (:raycaster ctx/world)
                                                                        (:explored-tile-corners ctx/world)
                                                                        (camera/position (:camera (:world-viewport ctx/graphics)))))
                            (graphics/draw-on-world-view! ctx/graphics
                                                          (fn []
                                                            (draw-before-entities!)
                                                            (render-entities!)
                                                            (draw-after-entities!)))
                            (stage/draw! stage)
                            (stage/act! stage)
                            (state/manual-tick (entity/state-obj @(:player-eid ctx/world)))
                            (update-mouseover-entity!)
                            (alter-var-root #'ctx/world assoc :paused? (pause-game?))
                            (when-not (:paused? ctx/world)
                              (alter-var-root #'ctx/world update-time (gdx/delta-time))
                              (update-potential-fields! ctx/world)
                              (tick-entities! ctx/world))

                            ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                            (remove-destroyed-entities! ctx/world)

                            (camera-controls!)
                            (window-controls!))

                          (resize [width height]
                            (graphics/resize! ctx/graphics width height))))))
