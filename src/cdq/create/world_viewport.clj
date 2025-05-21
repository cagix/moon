(ns cdq.create.world-viewport
  (:require [gdl.graphics :as graphics]))

(defn do! [{:keys [ctx/world-unit-scale
                   ctx/config]}]
  (graphics/world-viewport world-unit-scale
                           (:world-viewport config)))
