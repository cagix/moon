(ns cdq.component.render
  (:require [anvil.effect :as effect]
            [anvil.entity :as entity]
            [cdq.context :as world :refer [finished-ratio]]
            [clojure.component :as component]
            [gdl.context :as c]))

(defmethod component/render-effect :effects/target-all
  [_ {:keys [effect/source]} c]
  (let [source* @source]
    (doseq [target* (map deref (world/creatures-in-los-of-player c))]
      (c/line c
              (:position source*) #_(start-point source* target*)
              (:position target*)
              [1 0 0 0.5]))))

(defmethod component/render-effect :effects/target-entity
  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} c]
  (when target
    (let [source* @source
          target* @target]
      (c/line c
              (entity/start-point source* target*)
              (entity/end-point source* target* maxrange)
              (if (entity/in-range? source* target* maxrange)
                [1 0 0 0.5]
                [1 1 0 0.5])))))
