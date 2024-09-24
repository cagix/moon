(ns ^:no-doc core.entity.line-render
  (:require [core.component :as component :refer [defcomponent]]
            [core.entity :as entity]
            [core.graphics :as g]))

(defcomponent :entity/line-render
  {:let {:keys [thick? end color]}}
  (entity/render [_ entity* g _ctx]
    (let [position (:position entity*)]
      (if thick?
        (g/with-shape-line-width g 4 #(g/draw-line g position end color))
        (g/draw-line g position end color)))))

(defcomponent :tx/line-render
  (component/do! [[_ {:keys [start end duration color thick?]}] _ctx]
    [[:e/create
      start
      entity/effect-body-props
      #:entity {:line-render {:thick? thick? :end end :color color}
                :delete-after-duration duration}]]))
