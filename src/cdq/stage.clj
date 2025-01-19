(ns cdq.stage
  (:require [cdq.graphics :as graphics]
            [cdq.scene2d.stage :as stage]))

(defn mouse-on-actor? [stage]
  (let [[x y] (graphics/mouse-position (.getViewport stage))]
    (stage/hit stage x y true)))
