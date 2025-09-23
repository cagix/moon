(ns cdq.effects.target-entity
  (:require [cdq.entity :as entity]
            [clojure.math.vector2 :as v]))

; TODO use at projectile & also adjust rotation
(defn start-point [entity target*]
  (v/add (entity/position entity)
         (v/scale (v/direction (entity/position entity)
                               (entity/position target*))
                  (/ (:body/width (:entity/body entity)) 2))))

(defn end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (v/direction (entity/position entity)
                               (entity/position target*))
                  maxrange)))

(defn in-range? [entity target* maxrange]
  (< (- (float (v/distance (entity/position entity)
                           (entity/position target*)))
        (float (/ (:body/width (:entity/body entity))  2))
        (float (/ (:body/width (:entity/body target*)) 2)))
     (float maxrange)))
