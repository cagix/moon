(ns cdq.graphics.create.batch
  (:require [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]))

(defn create
  [graphics]
  (assoc graphics :graphics/batch (sprite-batch/create)))
