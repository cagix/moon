(ns anvil.effect.target-entity
  (:require [anvil.component :refer [applicable? handle]]
            [anvil.effect :refer [do-all! filter-applicable?]]
            [anvil.entity.body :as body]
            [anvil.world :as world]
            [gdl.math.vector :as v]
            [gdl.utils :refer [defmethods]]
            [gdl.db :as db]))

(defn in-range? [entity target* maxrange] ; == circle-collides?
  (< (- (float (v/distance (:position entity)
                           (:position target*)))
        (float (:radius entity))
        (float (:radius target*)))
     (float maxrange)))

; TODO use at projectile & also adjust rotation
(defn start-point [entity target*]
  (v/add (:position entity)
         (v/scale (body/direction entity target*)
                  (:radius entity))))

(defn end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (body/direction entity target*)
                  maxrange)))

(defmethods :effects/target-entity
  (applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as ctx}]
    (and target
         (seq (filter-applicable? ctx entity-effects))))

  (handle [[_ {:keys [maxrange entity-effects]}] {:keys [effect/source effect/target] :as ctx}]
    (let [source* @source
          target* @target]
      (if (in-range? source* target* maxrange)
        (do
         (world/line-render {:start (start-point source* target*)
                             :end (:position target*)
                             :duration 0.05
                             :color [1 0 0 0.75]
                             :thick? true})
         (do-all! ctx entity-effects))
        (world/audiovisual (end-point source* target* maxrange)
                           (db/build :audiovisuals/hit-ground))))))
