(ns cdq.start.pipeline.colors
  (:require [clojure.gdx.graphics.colors :as colors]))

(defn define-gdx-colors!
  [ctx]
  (colors/put! [["PRETTY_NAME" [0.84 0.8 0.52 1]]])
  ctx)
