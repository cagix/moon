(ns cdq.effect.target-entity
  (:require [cdq.entity :as entity]
            [cdq.context :as world]
            [cdq.effect-context :refer [do-all! filter-applicable?]]
            [gdl.context :as c]))

(defn applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as ctx}]
  (and target
       (seq (filter-applicable? ctx entity-effects))))

(defn useful?  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} _c]
  (entity/in-range? @source @target maxrange))

(defn handle [[_ {:keys [maxrange entity-effects]}] {:keys [effect/source effect/target] :as ctx} c]
  (let [source* @source
        target* @target]
    (if (entity/in-range? source* target* maxrange)
      (do
       (world/line-render c
                          {:start (entity/start-point source* target*)
                           :end (:position target*)
                           :duration 0.05
                           :color [1 0 0 0.75]
                           :thick? true})
       (do-all! c ctx entity-effects))
      (world/audiovisual c
                         (entity/end-point source* target* maxrange)
                         (c/build c :audiovisuals/hit-ground)))))

(defn render [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} c]
  (when target
    (let [source* @source
          target* @target]
      (c/line c
              (entity/start-point source* target*)
              (entity/end-point source* target* maxrange)
              (if (entity/in-range? source* target* maxrange)
                [1 0 0 0.5]
                [1 1 0 0.5])))))
