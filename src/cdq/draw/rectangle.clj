(ns cdq.draw.rectangle
  (:require [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]))

(defn do!
  [{:keys [ctx/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/rectangle! shape-drawer x y w h))
