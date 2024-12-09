(ns forge.entity.movement
  (:require [anvil.entity :as entity]
            [anvil.world :refer [max-delta-time world-delta cell-blocked?
                                 rectangle->cells cells->entities] :as world]
            [clojure.gdx.math.vector2 :as v]
            [malli.core :as m]))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [{:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (rectangle->cells body))]
    (and (not-any? #(cell-blocked? % z-order) cells*)
         (->> cells*
              cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (entity/collides? other-entity body)))))))))

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
(def ^:private max-speed (/ world/minimum-body-size
                            max-delta-time)) ; need to make var because m/schema would fail later if divide / is inside the schema-form

(def speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

(defn tick
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
    (let [movement (assoc movement :delta-time world-delta)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body body movement)
                        (move-body body movement))]
        (world/entity-position-changed eid)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))))))
