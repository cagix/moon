(ns cdq.effects.target-entity
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
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

(defn applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as effect-ctx}]
  (and target
       (seq (effect/filter-applicable? effect-ctx entity-effects))))

(defn useful? [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} _ctx]
  (in-range? @source @target maxrange))

(defn handle [[_ {:keys [maxrange entity-effects]}]
              {:keys [effect/source effect/target] :as effect-ctx}
              _ctx]
  (let [source* @source
        target* @target]
    (if (in-range? source* target* maxrange)
      [[:tx/spawn-line {:start (start-point source* target*)
                        :end (entity/position target*)
                        :duration 0.05
                        :color [1 0 0 0.75]
                        :thick? true}]
       [:tx/effect effect-ctx entity-effects]]
      [[:tx/audiovisual
        (end-point source* target* maxrange)
        :audiovisuals/hit-ground]])))
