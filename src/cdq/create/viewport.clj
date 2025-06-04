(ns cdq.create.viewport
  (:require [clojure.gdx :as gdx]))

(defn ui [_ctx config]
  (gdx/ui-viewport config))

(defn world [{:keys [ctx/world-unit-scale]} config]
  (gdx/world-viewport world-unit-scale config))
