(ns cdq.resize
  (:require [gdl.viewport :as viewport]))

(defn do! [{:keys [ctx/ui-viewport] :as ctx}
           width
           height]
  (viewport/update! ui-viewport)
  (viewport/update! (:world-viewport (:ctx/graphics ctx))))
