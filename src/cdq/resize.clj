(ns cdq.resize
  (:require [gdl.viewport :as viewport]))

(defn do! [{:keys [ctx/ui-viewport
                   ctx/world-viewport]}
           width
           height]
  (viewport/update! ui-viewport)
  (viewport/update! world-viewport))
