(ns cdq.gdx-app.resize
  (:require [clojure.gdx.utils.viewport :as viewport]))

(defn do!
  [{:keys [ctx/ui-viewport
           ctx/world-viewport]}
   width height]
  (viewport/update! ui-viewport    width height :center? true)
  (viewport/update! world-viewport width height :center? false))
