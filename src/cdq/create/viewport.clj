(ns cdq.create.viewport
  (:require [clojure.gdx :as gdx]))

(defn ui [ctx config]
  (assoc ctx :ctx/ui-viewport (gdx/ui-viewport config)))

(defn world [{:keys [ctx/world-unit-scale] :as ctx} config]
  (assoc ctx :ctx/world-viewport (gdx/world-viewport world-unit-scale config)))
