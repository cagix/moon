(ns ^:no-doc moon.entity.line-render
  (:require [moon.component :refer [defc] :as component]
            [moon.graphics :as g]
            [moon.entity :as entity]))

(defc :entity/line-render
  {:let {:keys [thick? end color]}}
  (entity/render [_ entity]
    (let [position (:position entity)]
      (if thick?
        (g/with-shape-line-width 4 #(g/draw-line position end color))
        (g/draw-line position end color)))))
