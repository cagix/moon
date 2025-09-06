(ns cdq.create.shape-drawer
  (:require [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.texture :as texture]))

(defn do!
  [{:keys [ctx/shape-drawer-texture
           ctx/batch]
    :as ctx}]
  (assoc ctx :ctx/shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))))
