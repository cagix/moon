(ns ^:no-doc anvil.entity.string-effect
  (:require [cdq.context :refer [stopped?]]
            [clojure.component :as component :refer [defcomponent]]
            [gdl.context :as c]))

(defcomponent :entity/string-effect
  (component/tick [[k {:keys [counter]}] eid c]
    (when (stopped? c counter)
      (swap! eid dissoc k)))

  (component/render-above [[_ {:keys [text]}] entity c]
    (let [[x y] (:position entity)]
      (c/draw-text c
                   {:text text
                    :x x
                    :y (+ y
                          (:half-height entity)
                          (c/pixels->world-units c 5))
                    :scale 2
                    :up? true}))))
