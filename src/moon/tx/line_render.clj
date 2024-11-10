(ns moon.tx.line-render
  (:require [moon.body :as body]
            [moon.world.entities :as entities]))

(defn handle [{:keys [start end duration color thick?]}]
  (entities/create
     start
     body/effect-body-props
     #:entity {:line-render {:thick? thick? :end end :color color}
               :delete-after-duration duration})
  nil)
