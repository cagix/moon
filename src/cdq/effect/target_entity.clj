(ns cdq.effect.target-entity
  (:require [cdq.context :as world]
            [cdq.effect-context :refer [do-all! filter-applicable?]]
            [cdq.entity :as entity]
            [gdl.context :as c]
            [clojure.gdx.math.vector2 :as v]))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity target*]
  (v/add (:position entity)
         (v/scale (entity/direction entity target*)
                  (:radius entity))))

(defn- end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (entity/direction entity target*)
                  maxrange)))

(defn- in-range? [entity target* maxrange] ; == circle-collides?
  (< (- (float (v/distance (:position entity)
                           (:position target*)))
        (float (:radius entity))
        (float (:radius target*)))
     (float maxrange)))

(defn applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as ctx}]
  (and target
       (seq (filter-applicable? ctx entity-effects))))

(defn useful?  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} _c]
  (in-range? @source @target maxrange))

(defn handle [[_ {:keys [maxrange entity-effects]}] {:keys [effect/source effect/target] :as ctx} c]
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

(defn render [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} c]
  (when target
    (let [source* @source
          target* @target]
      (c/line c
              (start-point source* target*)
              (end-point source* target* maxrange)
              (if (in-range? source* target* maxrange)
                [1 0 0 0.5]
                [1 1 0 0.5])))))
