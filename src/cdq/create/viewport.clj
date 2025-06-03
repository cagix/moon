(ns cdq.create.viewport
  (:require [clojure.gdx :as gdx]))

(defn ui [{:keys [ctx/config] :as ctx}]
  (assoc ctx :ctx/ui-viewport (gdx/ui-viewport (:ui-viewport config))))

(defn world [{:keys [ctx/config
                     ctx/world-unit-scale] :as ctx}]
  (assoc ctx :ctx/world-viewport (gdx/world-viewport world-unit-scale (:world-viewport config))))
