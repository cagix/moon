(ns cdq.create.graphics.shape-drawer
  (:require [clojure.utils]
            [gdl.graphics.texture :as texture]
            [clojure.gdx.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/batch
           graphics/shape-drawer-texture]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer (clojure.gdx.shape-drawer/create batch (texture/region shape-drawer-texture 1 0 1 1))))
