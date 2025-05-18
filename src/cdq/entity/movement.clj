(ns cdq.entity.movement
  (:require [cdq.ctx :as ctx]
            [cdq.cell :as cell]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.malli :as m]
            [cdq.math :as math]
            [cdq.utils :refer [defcomponent]]
            [cdq.vector2 :as v]))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [grid {:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (grid/rectangle->cells grid body))]
    (and (not-any? #(cell/blocked? % z-order) cells*)
         (->> cells*
              grid/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (math/overlaps? other-entity body)))))))))

(defn- try-move [grid body movement]
  (let [new-body (move-body body movement)]
    (when (valid-position? grid new-body)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [grid body {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move grid body movement)
        (try-move grid body (assoc movement :direction [xdir 0]))
        (try-move grid body (assoc movement :direction [0 ydir])))))

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ ctx/minimum-size ctx/max-delta)) ; need to make var because s/schema would fail later if divide / is inside the schema-form

(def ^:private speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

(defcomponent :entity/movement
  (entity/tick! [[_ {:keys [direction
                            speed
                            rotate-in-movement-direction?]
                     :as movement}]
                 eid]
    (assert (m/validate speed-schema speed)
            (pr-str speed))
    (assert (or (zero? (v/length direction))
                (v/normalised? direction))
            (str "cannot understand direction: " (pr-str direction)))
    (when-not (or (zero? (v/length direction))
                  (nil? speed)
                  (zero? speed))
      (let [movement (assoc movement :delta-time ctx/delta-time)
            body @eid]
        (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                          (try-move-solid-body ctx/grid body movement)
                          (move-body body movement))]
          [[:tx/move-entity eid body direction rotate-in-movement-direction?]])))))
