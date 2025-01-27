(ns cdq.world
  (:require cdq.graphics
            [cdq.db :as db]
            [clojure.utils :refer [define-order safe-merge]]
            [cdq.graphics.shape-drawer :as sd]
            [cdq.timer :as timer]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.ui.button-group :as button-group]
            [cdq.stage :as stage]
            [cdq.info :as info]
            [cdq.math.vector2 :as v]
            [cdq.ui :as ui]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            cdq.time
            [clojure.gdx.audio.sound :as sound]))

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-size 0.39) ; == spider smallest creature size.

(def z-orders [:z-order/on-ground
               :z-order/ground
               :z-order/flying
               :z-order/effect])

(def render-z-order (define-order z-orders))

(defprotocol World
  (spawn-entity [_ position body components]))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn spawn-audiovisual [c position {:keys [tx/sound entity/animation]}]
  (sound/play sound)
  (spawn-entity c
                position
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

(defn spawn-creature [{:keys [cdq/db] :as c}
                      {:keys [position creature-id components]}]
  (let [props (db/build db creature-id c)]
    (spawn-entity c
                  position
                  (->body (:entity/body props))
                  (-> props
                      (dissoc :entity/body)
                      (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                      (safe-merge components)))))

(defn spawn-item [c position item]
  (spawn-entity c
                position
                {:width 0.75
                 :height 0.75
                 :z-order :z-order/on-ground}
                {:entity/image (:entity/image item)
                 :entity/item item
                 :entity/clickable {:type :clickable/item
                                    :text (:property/pretty-name item)}}))

(defn delayed-alert [{:keys [cdq.context/elapsed-time] :as c}
                     position
                     faction
                     duration]
  (spawn-entity c
                position
                effect-body-props
                {:entity/alert-friendlies-after-duration
                 {:counter (timer/create elapsed-time duration)
                  :faction faction}}))

(defn line-render [c {:keys [start end duration color thick?]}]
  (spawn-entity c
                start
                effect-body-props
                #:entity {:line-render {:thick? thick? :end end :color color}
                          :delete-after-duration duration}))

(defn projectile-size [projectile]
  {:pre [(:entity/image projectile)]}
  (first (:world-unit-dimensions (:entity/image projectile))))

(defn spawn-projectile [c
                        {:keys [position direction faction]}
                        {:keys [entity/image
                                projectile/max-range
                                projectile/speed
                                entity-effects
                                projectile/piercing?] :as projectile}]
  (let [size (projectile-size projectile)]
    (spawn-entity c
                  position
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

(defn nearest-enemy [{:keys [cdq.context/grid]} entity]
  (grid/nearest-entity @(grid (entity/tile entity))
                       (entity/enemy entity)))

(defn get-inventory [c]
  (get (:windows (:cdq.context/stage c)) :inventory-window))

(defn get-action-bar [c]
  (let [group (:ui/action-bar (:action-bar-table (:cdq.context/stage c)))]
    {:horizontal-group group
     :button-group (actor/user-object (group/find-actor group "action-bar/button-group"))}))

(defn- action-bar-add-skill [c {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar c)
        button (ui/image-button image (fn []) {:scale 2})]
    (actor/set-id button id)
    (ui/add-tooltip! button #(info/text % skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (group/add-actor! horizontal-group button)
    (button-group/add button-group button)
    nil))

(defn- action-bar-remove-skill [c {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar c)
        button (get horizontal-group id)]
    (actor/remove button)
    (button-group/remove button-group button)
    nil))

(defn selected-skill [c]
  (when-let [skill-button (button-group/checked (:button-group (get-action-bar c)))]
    (actor/user-object skill-button)))

(defn show-player-msg [{:keys [cdq.context/player-message]} text]
  (swap! player-message assoc :text text :counter 0))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [cdq.graphics/ui-viewport] :as c}
                  {:keys [title text button-text on-click]}]
  (assert (not (::modal (:cdq.context/stage c))))
  (stage/add-actor (:cdq.context/stage c)
                   (ui/window {:title title
                               :rows [[(ui/label text)]
                                      [(ui/text-button button-text
                                                       (fn []
                                                         (actor/remove (::modal (:cdq.context/stage c)))
                                                         (on-click)))]]
                               :id ::modal
                               :modal? true
                               :center-position [(/ (:width  ui-viewport) 2)
                                                 (* (:height ui-viewport) (/ 3 4))]
                               :pack? true})))

(defn world-item? [c]
  (not (stage/mouse-on-actor? (:cdq.context/stage c))))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.
(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn item-place-position [{:keys [cdq.graphics/world-viewport] :as c} entity]
  (placement-point (:position entity)
                   (cdq.graphics/world-mouse-position world-viewport)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn draw-body-rect [sd entity color]
  (let [[x y] (:left-bottom entity)]
    (sd/rectangle sd x y (:width entity) (:height entity) color)))

(defn player-movement-vector []
  (let [r (when (input/key-pressed? :d) [1  0])
        l (when (input/key-pressed? :a) [-1 0])
        u (when (input/key-pressed? :w) [0  1])
        d (when (input/key-pressed? :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn add-skill [c eid {:keys [property/id] :as skill}]
  {:pre [(not (entity/has-skill? @eid skill))]}
  (when (:entity/player? @eid)
    (action-bar-add-skill c skill))
  (swap! eid assoc-in [:entity/skills id] skill))

(defn remove-skill [c eid {:keys [property/id] :as skill}]
  {:pre [(entity/has-skill? @eid skill)]}
  (when (:entity/player? @eid)
    (action-bar-remove-skill c skill))
  (swap! eid update :entity/skills dissoc id))
