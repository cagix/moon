(ns moon.entity.line-render
  (:require [moon.component :refer [defc] :as component]
            [moon.graphics.shape-drawer :as sd]
            [moon.entity :as entity]))

(defc :entity/line-render
  {:let {:keys [thick? end color]}}
  (entity/render [_ entity]
    (let [position (:position entity)]
      (if thick?
        (sd/with-line-width 4 #(sd/line position end color))
        (sd/line position end color)))))
