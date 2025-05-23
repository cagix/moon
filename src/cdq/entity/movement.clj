(ns cdq.entity.movement
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.utils :refer [defcomponent]]
            [cdq.vector2 :as v]))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- try-move [ctx body movement]
  (let [new-body (move-body body movement)]
    (when (g/valid-position? ctx new-body)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [ctx body {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move ctx body movement)
        (try-move ctx body (assoc movement :direction [xdir 0]))
        (try-move ctx body (assoc movement :direction [0 ydir])))))

(defcomponent :entity/movement
  (entity/tick! [[_ {:keys [direction
                            speed
                            rotate-in-movement-direction?]
                     :as movement}]
                 eid
                 {:keys [ctx/delta-time] :as ctx}]
    (assert (<= 0 speed ctx/max-speed)
            (pr-str speed))
    (assert (or (zero? (v/length direction))
                (v/normalised? direction))
            (str "cannot understand direction: " (pr-str direction)))
    (when-not (or (zero? (v/length direction))
                  (nil? speed)
                  (zero? speed))
      (let [movement (assoc movement :delta-time delta-time)
            body @eid]
        (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                          (try-move-solid-body ctx body movement)
                          (move-body body movement))]
          [[:tx/move-entity eid body direction rotate-in-movement-direction?]])))))
