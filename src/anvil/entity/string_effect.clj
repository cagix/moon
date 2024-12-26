(ns ^:no-doc anvil.entity.string-effect
  (:require [anvil.component :as component]
            [cdq.context :refer [stopped?]]
            [gdl.context :as c]))

(defmethods :entity/string-effect
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
