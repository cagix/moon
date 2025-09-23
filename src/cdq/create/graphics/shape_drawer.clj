(ns cdq.create.graphics.shape-drawer
  (:require [clojure.utils]
            [gdl.graphics.texture :as texture]
            [gdl.impl.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/batch
           graphics/shape-drawer-texture]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer (gdl.impl.shape-drawer/create batch (texture/region shape-drawer-texture 1 0 1 1))))
