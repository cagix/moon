(ns cdq.entity.movement
  (:require [cdq.gdx.math.vector2 :as v]
            [cdq.world.grid :as grid]
            [clojure.utils :as utils]))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (update body :body/position move-position movement))

(defn- try-move [grid body entity-id movement]
  (let [new-body (move-body body movement)]
    (when (grid/valid-position? grid new-body entity-id)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [grid body entity-id {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move grid body entity-id movement)
        (try-move grid body entity-id (assoc movement :direction [xdir 0]))
        (try-move grid body entity-id (assoc movement :direction [0 ydir])))))

(defn tick!
  [{:keys [direction
           speed
           rotate-in-movement-direction?]
    :as movement}
   eid
   {:keys [ctx/world]}]
  (assert (<= 0 speed (:world/max-speed world))
          (pr-str speed))
  (assert (or (zero? (v/length direction))
              (utils/nearly-equal? 1 (v/length direction)))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v/length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time (:world/delta-time world))
          body (:entity/body @eid)]
      (when-let [body (if (:body/collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body (:world/grid world) body (:entity/id @eid) movement)
                        (move-body body movement))]
        [[:tx/move-entity eid body direction rotate-in-movement-direction?]]))))
