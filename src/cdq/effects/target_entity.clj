(ns cdq.effects.target-entity
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.draw :as draw]
            [cdq.db :as db]
            [cdq.utils :refer [defcomponent]]
            [cdq.vector2 :as v]))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity target*]
  (v/add (:position entity)
         (v/scale (v/direction (:position entity)
                               (:position target*))
                  (:radius entity))))

(defn- end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (v/direction (:position entity)
                               (:position target*))
                  maxrange)))

(defcomponent :effects/target-entity
  (effect/applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as effect-ctx}]
    (and target
         (seq (effect/filter-applicable? effect-ctx entity-effects))))

  (effect/useful? [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} _ctx]
    (entity/in-range? @source @target maxrange))

  (effect/handle [[_ {:keys [maxrange entity-effects]}]
                  {:keys [effect/source effect/target] :as effect-ctx}
                  {:keys [ctx/db] :as ctx}]
    (let [source* @source
          target* @target]
      (if (entity/in-range? source* target* maxrange)
        [[:tx/spawn-line {:start (start-point source* target*)
                          :end (:position target*)
                          :duration 0.05
                          :color [1 0 0 0.75]
                          :thick? true}]
         [:tx/effect effect-ctx entity-effects]]
        [[:tx/audiovisual
          (end-point source* target* maxrange)
          (db/build db :audiovisuals/hit-ground ctx)]])))

  (effect/render [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} ctx]
    (when target
      (let [source* @source
            target* @target]
        (draw/line ctx
                   (start-point source* target*)
                   (end-point source* target* maxrange)
                   (if (entity/in-range? source* target* maxrange)
                     [1 0 0 0.5]
                     [1 1 0 0.5]))))))
