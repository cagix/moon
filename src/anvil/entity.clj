(ns anvil.entity
  (:require [anvil.animation :as animation]
            [anvil.app :refer [world-viewport-width world-viewport-height]]
            [anvil.audio :refer [play-sound]]
            [anvil.content-grid :as content-grid]
            [anvil.db :as db]
            [anvil.graphics :refer [world-camera]]
            [anvil.grid :as grid]
            [anvil.inventory :as inventory]
            [anvil.raycaster :refer [ray-blocked?]]
            [anvil.skills :as skills]
            [anvil.stat :as stat]
            [anvil.time :refer [timer]]
            [clojure.component :refer [defsystem]]
            [clojure.gdx.graphics.camera :as cam]
            [clojure.gdx.math.vector2 :as v]
            [clojure.utils :refer [define-order safe-merge unique-number!]]
            [reduce-fsm :as fsm]))

(def ^:private npc-fsm
  (fsm/fsm-inc
   [[:npc-sleeping
     :kill -> :npc-dead
     :stun -> :stunned
     :alert -> :npc-idle]
    [:npc-idle
     :kill -> :npc-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :movement-direction -> :npc-moving]
    [:npc-moving
     :kill -> :npc-dead
     :stun -> :stunned
     :timer-finished -> :npc-idle]
    [:active-skill
     :kill -> :npc-dead
     :stun -> :stunned
     :action-done -> :npc-idle]
    [:stunned
     :kill -> :npc-dead
     :effect-wears-off -> :npc-idle]
    [:npc-dead]]))

(def ^:private player-fsm
  (fsm/fsm-inc
   [[:player-idle
     :kill -> :player-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :pickup-item -> :player-item-on-cursor
     :movement-input -> :player-moving]
    [:player-moving
     :kill -> :player-dead
     :stun -> :stunned
     :no-movement-input -> :player-idle]
    [:active-skill
     :kill -> :player-dead
     :stun -> :stunned
     :action-done -> :player-idle]
    [:stunned
     :kill -> :player-dead
     :effect-wears-off -> :player-idle]
    [:player-item-on-cursor
     :kill -> :player-dead
     :stun -> :stunned
     :drop-item -> :player-idle
     :dropped-item -> :player-idle]
    [:player-dead]]))

(declare player-eid
         ids)

(defn all-entities []
  (vals ids))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (cam/position (world-camera))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float world-viewport-width)  2)))
     (<= ydist (inc (/ (float world-viewport-height) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? target))
       (not (and los-checks?
                 (ray-blocked? (:position source) (:position target))))))

(defn- set-cells! [eid]
  (let [cells (grid/rectangle->cells @eid)]
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
(defn- rectangle->occupied-cells [{:keys [left-bottom width height] :as rectangle}]
  (if (or (> (float width) 1) (> (float height) 1))
    (grid/rectangle->cells rectangle)
    [(grid/get [(int (+ (float (left-bottom 0)) (/ (float width) 2)))
                (int (+ (float (left-bottom 1)) (/ (float height) 2)))])]))

(defn- set-occupied-cells! [eid]
  (let [cells (rectangle->occupied-cells @eid)]
    (doseq [cell cells]
      (assert (not (get (:occupied @cell) eid)))
      (swap! cell update :occupied conj eid))
    (swap! eid assoc ::occupied-cells cells)))

(defn- remove-from-occupied-cells! [eid]
  (doseq [cell (::occupied-cells @eid)]
    (assert (get (:occupied @cell) eid))
    (swap! cell update :occupied disj eid)))

(defn- grid-add-entity [eid]
  (set-cells! eid)
  (when (:collides? @eid)
    (set-occupied-cells! eid)))

(defn- grid-remove-entity [eid]
  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn- grid-entity-position-changed [eid]
  (remove-from-cells! eid)
  (set-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! eid)))

(defn- entity-ids-add-entity [eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (alter-var-root #'ids assoc id eid)))

(defn- entity-ids-remove-entity [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? ids id))
    (alter-var-root #'ids dissoc id)))

(declare content-grid)

(defn active-entities []
  (content-grid/active-entities content-grid @player-eid))

(defn- add-entity [eid]
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (content-grid/add-entity content-grid eid)
  (entity-ids-add-entity   eid)
  (grid-add-entity         eid))

(defn remove-entity [eid]
  (content-grid/remove-entity eid)
  (entity-ids-remove-entity   eid)
  (grid-remove-entity         eid))

(defn position-changed [eid]
  (content-grid/entity-position-changed content-grid eid)
  (grid-entity-position-changed         eid))

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

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-body-size 0.39) ; == spider smallest creature size.

(def ^:private z-orders [:z-order/on-ground
                         :z-order/ground
                         :z-order/flying
                         :z-order/effect])

(def render-z-order (define-order z-orders))

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
  (assert (>= width  (if collides? minimum-body-size 0)))
  (assert (>= height (if collides? minimum-body-size 0)))
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

(defsystem ->v)
(defmethod ->v :default [[_ v]]
  v)

(defmethod ->v :entity/delete-after-duration [[_ duration]]
  (timer duration))

(defmethod ->v :entity/hp [[_ v]]
  [v v])

(defmethod ->v :entity/mana [[_ v]]
  [v v])

(defmethod ->v :entity/projectile-collision [[_ v]]
  (assoc v :already-hit-bodies #{}))

(defmethod ->v :stunned [[_ eid duration]]
  {:eid eid
   :counter (timer duration)})

(defmethod ->v :player-moving [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector})

(defmethod ->v :player-item-on-cursor [[_ eid item]]
  {:eid eid
   :item item})

(defmethod ->v :player-idle [[_ eid]]
  {:eid eid})

(defmethod ->v :npc-sleeping [[_ eid]]
  {:eid eid})

(defmethod ->v :npc-moving [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer (* (stat/->value @eid :entity/reaction-time) 0.016))})

(defn- create-vs [components]
  (reduce (fn [m [k v]]
            (assoc m k (->v [k v])))
          {}
          components))

(defsystem create)
(defmethod create :default [_ eid])

(defmethod create :entity/skills [[k skills] eid]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (swap! eid skills/add skill)))

(defmethod create :entity/inventory [[k items] eid]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (inventory/pickup-item eid item)))

(defmethod create :entity/delete-after-animation-stopped? [_ eid]
  (-> @eid :entity/animation :looping? not assert))

(defmethod create :entity/animation [[_ animation] eid]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

; fsm throws when initial-state is not part of states, so no need to assert initial-state
; initial state is nil, so associng it. make bug report at reduce-fsm?
(defn- ->init-fsm [fsm initial-state]
  (assoc (fsm initial-state nil) :state initial-state))

(defmethod create :entity/fsm [[k {:keys [fsm initial-state]}] eid]
  (swap! eid assoc
         k (->init-fsm (case fsm
                         :fsms/player player-fsm
                         :fsms/npc npc-fsm)
                       initial-state)
         initial-state (->v [initial-state eid])))

(defn- spawn-entity [position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (safe-merge (-> components
                                      (assoc :entity/id (unique-number!))
                                      create-vs))))]
    (add-entity eid)
    (doseq [component @eid]
      (create component eid))
    eid))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn audiovisual [position {:keys [tx/sound entity/animation]}]
  (play-sound sound)
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

(defn creature [{:keys [position creature-id components]}]
  (let [props (db/build creature-id)]
    (spawn-entity position
                  (->body (:entity/body props))
                  (-> props
                      (dissoc :entity/body)
                      (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                      (safe-merge components)))))

(defn item [position item]
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
                 {:counter (timer duration)
                  :faction faction}}))

(defn line-render [{:keys [start end duration color thick?]}]
  (spawn-entity start
                effect-body-props
                #:entity {:line-render {:thick? thick? :end end :color color}
                          :delete-after-duration duration}))

(defn projectile-size [projectile]
  {:pre [(:entity/image projectile)]}
  (first (:world-unit-dimensions (:entity/image projectile))))

(defn projectile [{:keys [position direction faction]}
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

(def mouseover-eid nil)

(defn mouseover-entity []
  (and mouseover-eid
       @mouseover-eid))

(defn creatures-in-los-of-player []
  (->> (active-entities)
       (filter #(:entity/species @%))
       (filter #(line-of-sight? @player-eid @%))
       (remove #(:entity/player? @%))))
