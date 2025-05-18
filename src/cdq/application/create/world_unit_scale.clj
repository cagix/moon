(ns cdq.application.create.world-unit-scale
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]))

(defn do! []
  (bind-root #'ctx/world-unit-scale (float (/ (:tile-size ctx/config)))))
