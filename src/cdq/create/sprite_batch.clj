(ns cdq.create.sprite-batch
  (:require [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]))

(defn do! [ctx]
  (assoc ctx :ctx/batch (sprite-batch/create)))
