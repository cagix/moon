(ns cdq.graphics.sprite-batch
  (:require [com.badlogic.gdx.graphics :as graphics]))

(defn create [{:keys [graphics/core]
               :as graphics}]
  (assoc graphics :graphics/batch (graphics/sprite-batch core)))
