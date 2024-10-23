(ns world.entity.line
  (:require [component.core :refer [defc]]
            [component.tx :as tx]
            [gdx.graphics :as g]
            [world.entity :as entity]))

(defc :entity/line-render
  {:let {:keys [thick? end color]}}
  (entity/render [_ entity]
    (let [position (:position entity)]
      (if thick?
        (g/with-shape-line-width 4 #(g/draw-line position end color))
        (g/draw-line position end color)))))

(defc :tx/line-render
  (tx/handle [[_ {:keys [start end duration color thick?]}]]
    [[:e/create
      start
      entity/effect-body-props
      #:entity {:line-render {:thick? thick? :end end :color color}
                :delete-after-duration duration}]]))
