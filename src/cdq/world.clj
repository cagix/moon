(ns cdq.world
  (:require [cdq.audio.sound :as sound]
            [cdq.context :as context]
            [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.grid :as grid]
            [cdq.info :as info]
            [cdq.input :as input]
            [cdq.math.vector2 :as v]
            [cdq.timer :as timer]
            [cdq.ui :as ui]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.stage :as stage]
            [cdq.utils :refer [define-order safe-merge]])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Button ButtonGroup)))

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

(defn- create-vs [components context]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v] context)))
          {}
          components))

(def id-counter (atom 0))

(defn- spawn-entity [context position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (safe-merge (-> components
                                      (assoc :entity/id (swap! id-counter inc))
                                      (create-vs context)))))]
    (doseq [component context]
      (context/add-entity component eid))
    (doseq [component @eid]
      (entity/create! component eid context))
    eid))

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

(defn get-inventory [{:keys [cdq.context/stage]}]
  (get (:windows stage) :inventory-window))

(defn get-action-bar [{:keys [cdq.context/stage]}]
  (let [group (:ui/action-bar (:action-bar-table stage))]
    {:horizontal-group group
     :button-group (actor/user-object (group/find-actor group "action-bar/button-group"))}))

(defn selected-skill [c]
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-action-bar c)))]
    (actor/user-object skill-button)))

(defn world-item? [{:keys [cdq.context/stage]}]
  (not (stage/mouse-on-actor? stage)))

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
                   (graphics/world-mouse-position world-viewport)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn player-movement-vector []
  (let [r (when (input/key-pressed? :d) [1  0])
        l (when (input/key-pressed? :a) [-1 0])
        u (when (input/key-pressed? :w) [0  1])
        d (when (input/key-pressed? :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn- action-bar-add-skill [c {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar c)
        button (ui/image-button image (fn []) {:scale 2})]
    (actor/set-id button id)
    (ui/add-tooltip! button #(info/text % skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (group/add-actor! horizontal-group button)
    (ButtonGroup/.add button-group ^Button button)
    nil))

(defn- action-bar-remove-skill [c {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar c)
        button (get horizontal-group id)]
    (actor/remove button)
    (ButtonGroup/.remove button-group ^Button button)
    nil))

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
