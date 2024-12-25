(ns ^:no-doc anvil.effect.target-entity
  (:require [anvil.component :as component]
            [anvil.effect :refer [do-all! filter-applicable?]]
            [anvil.entity :as entity]
            [anvil.world :as world]
            [gdl.context :as c]
            [gdl.math.vector :as v]))

(defn- in-range? [entity target* maxrange] ; == circle-collides?
  (< (- (float (v/distance (:position entity)
                           (:position target*)))
        (float (:radius entity))
        (float (:radius target*)))
     (float maxrange)))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity target*]
  (v/add (:position entity)
         (v/scale (entity/direction entity target*)
                  (:radius entity))))

(defn- end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (entity/direction entity target*)
                  maxrange)))

(defmethods :effects/target-entity
  (component/applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as ctx}]
    (and target
         (seq (filter-applicable? ctx entity-effects))))

  (component/useful?  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]}]
    (in-range? @source @target maxrange))

  (component/handle [[_ {:keys [maxrange entity-effects]}] {:keys [effect/source effect/target] :as ctx} c]
    (let [source* @source
          target* @target]
      (if (in-range? source* target* maxrange)
        (do
         (world/line-render c
                            {:start (start-point source* target*)
                             :end (:position target*)
                             :duration 0.05
                             :color [1 0 0 0.75]
                             :thick? true})
         (do-all! c ctx entity-effects))
        (world/audiovisual c
                           (end-point source* target* maxrange)
                           (c/build c :audiovisuals/hit-ground)))))

  (component/render-effect [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} c]
    (when target
      (let [source* @source
            target* @target]
        (c/line c
                (start-point source* target*)
                (end-point source* target* maxrange)
                (if (in-range? source* target* maxrange)
                  [1 0 0 0.5]
                  [1 1 0 0.5]))))))
