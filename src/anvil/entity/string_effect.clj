(ns ^:no-doc anvil.entity.string-effect
  (:require [anvil.component :as component]
            [anvil.world :refer [stopped?]]
            [gdl.context :as c]))

(defmethods :entity/string-effect
  (component/tick [[k {:keys [counter]}] eid]
    (when (stopped? counter)
      (swap! eid dissoc k)))

  (component/render-above [[_ {:keys [text]}] entity]
    (let [[x y] (:position entity)]
      (c/draw-text (c/get-ctx)
                   {:text text
                    :x x
                    :y (+ y
                          (:half-height entity)
                          (c/pixels->world-units 5))
                    :scale 2
                    :up? true}))))
