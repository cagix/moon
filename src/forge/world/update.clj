(ns forge.world.update
  (:require [anvil.animation :as animation]
            [anvil.body :as body]
            [anvil.db :as db]
            [anvil.controls :as controls]
            [anvil.effect :as effect]
            [anvil.entity :as entity :refer [player-eid mouseover-entity mouseover-eid line-of-sight? render-z-order]]
            [anvil.error :as error]
            [anvil.fsm :as fsm]
            [anvil.graphics :as g :refer [world-mouse-position]]
            [anvil.grid :as grid]
            [anvil.modifiers :as mods]
            [anvil.stage :as stage]
            [anvil.time :as time :refer [stopped?]]
            [anvil.level :as level :refer [explored-tile-corners]]
            [clojure.component :refer [defsystem]]
            [clojure.gdx.graphics :refer [delta-time]]
            [clojure.gdx.math.vector2 :as v]
            [clojure.utils :refer [bind-root sort-by-order find-first]]
            [forge.world.potential-fields :refer [update-potential-fields!]]
            [malli.core :as m]))

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       grid/circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [{:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (grid/rectangle->cells body))]
    (and (not-any? #(grid/cell-blocked? % z-order) cells*)
         (->> cells*
              grid/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (body/collides? other-entity body)))))))))

(defn- try-move [body movement]
  (let [new-body (move-body body movement)]
    (when (valid-position? new-body)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [body {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move body movement)
        (try-move body (assoc movement :direction [xdir 0]))
        (try-move body (assoc movement :direction [0 ydir])))))

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ entity/minimum-body-size
                            time/max-delta)) ; need to make var because m/schema would fail later if divide / is inside the schema-form

(def speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defsystem tick)
(defmethod tick :default [_ eid])

(defmethod tick :entity/animation [[k animation] eid]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation time/delta)))))

(defmethod tick :entity/delete-after-animation-stopped? [_ eid]
  (when (animation/stopped? (:entity/animation @eid))
    (swap! eid assoc :entity/destroyed? true)))

(defmethod tick :entity/movement
  [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
   eid]
  (assert (m/validate speed-schema speed)
          (pr-str speed))
  (assert (or (zero? (v/length direction))
              (v/normalised? direction))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v/length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time time/delta)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body body movement)
                        (move-body body movement))]
        (entity/position-changed eid)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))))))

(defmethod tick :entity/alert-friendlies-after-duration [[_ {:keys [counter faction]}] eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius (:position @eid) faction)]
      (fsm/event friendly-eid :alert))))

(defmethod tick :entity/delete-after-duration [[_ counter] eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)))

(defmethod tick :entity/string-effect [[k {:keys [counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)))

(defmethod tick :entity/temp-modifier [[k {:keys [modifiers counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)
    (swap! eid mods/remove modifiers)))

(defmethod tick :entity/projectile-collision
  [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid]
  ; TODO this could be called from body on collision
  ; for non-solid
  ; means non colliding with other entities
  ; but still collding with other stuff here ? o.o
  (let [entity @eid
        cells* (map deref (grid/rectangle->cells entity)) ; just use cached-touched -cells
        hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                     (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                           (:entity/faction @%))
                                     (:collides? @%)
                                     (body/collides? entity @%))
                               (grid/cells->entities cells*))
        destroy? (or (and hit-entity (not piercing?))
                     (some #(grid/cell-blocked? % (:z-order entity)) cells*))]
    (when destroy?
      (swap! eid assoc :entity/destroyed? true))
    (when hit-entity
      (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
    (when hit-entity
      (effect/do-all! {:effect/source eid
                       :effect/target hit-entity}
                      entity-effects))))

(defmethod tick :entity/skills [[k skills] eid]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (stopped? cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))

(defmethod tick :stunned [[_ {:keys [counter]}] eid]
  (when (stopped? counter)
    (fsm/event eid :effect-wears-off)))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "entity-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn- tick-entities [entities]
  (run! tick-entity entities))

(defn- time-update []
  (let [delta-ms (min (delta-time) time/max-delta)]
    (alter-var-root #'time/elapsed + delta-ms)
    (bind-root time/delta delta-ms)))

(defn- calculate-eid []
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (grid/point->entities
                      (world-mouse-position)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? player @%))
         first)))

(defn- update-mouseover-entity []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (calculate-eid))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root mouseover-eid new-eid)))

(defsystem destroy)
(defmethod destroy :default [_ eid])

(defmethod destroy :entity/destroy-audiovisual [[_ audiovisuals-id] eid]
  (entity/audiovisual (:position @eid)
                      (db/build audiovisuals-id)))

(defn- remove-destroyed-entities []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (entity/all-entities))]
    (entity/remove-entity eid)
    (doseq [component @eid]
      (destroy component eid))))

(defsystem manual-tick)
(defmethod manual-tick :default [_])

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defmethod pause-game? :stunned [_] false)

(defn update-world []
  (manual-tick (fsm/state-obj @player-eid))
  (update-mouseover-entity) ; this do always so can get debug info even when game not running
  (bind-root time/paused? (or error/throwable
                              (and pausing?
                                   (pause-game? (fsm/state-obj @player-eid))
                                   (not (controls/unpaused?)))))
  (when-not time/paused?
    (time-update)
    (let [entities (entity/active-entities)]
      (update-potential-fields! entities)
      (try (tick-entities entities)
           (catch Throwable t
             (stage/error-window! t)
             (bind-root error/throwable t)))))
  (remove-destroyed-entities)) ; do not pause this as for example pickup item, should be destroyed.
