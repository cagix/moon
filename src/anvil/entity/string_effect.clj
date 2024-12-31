(ns ^:no-doc anvil.entity.string-effect
  (:require [anvil.entity :as entity]
            [cdq.context :refer [stopped?]]
            [clojure.utils :refer [defmethods]]
            [gdl.context :as c]))

(defmethods :entity/string-effect
  (entity/tick [[k {:keys [counter]}] eid c]
    (when (stopped? c counter)
      (swap! eid dissoc k)))

  (entity/render-above [[_ {:keys [text]}] entity c]
    (let [[x y] (:position entity)]
      (c/draw-text c
                   {:text text
                    :x x
                    :y (+ y
                          (:half-height entity)
                          (c/pixels->world-units c 5))
                    :scale 2
                    :up? true}))))
