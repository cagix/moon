(ns cdq.application.resize
  (:require [gdl.graphics.viewport :as viewport]))

(defn do! [{:keys [ctx/ui-viewport
                   ctx/world-viewport]}]
  (viewport/update! ui-viewport)
  (viewport/update! world-viewport))
