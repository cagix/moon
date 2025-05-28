(ns cdq.resize
  (:require [gdl.viewport :as viewport]))

(defn do! [{:keys [ctx/ui-viewport] :as ctx}
           width
           height]
  (viewport/update! ui-viewport width height)
  (viewport/update! (:world-viewport (:ctx/graphics ctx)) width height))
