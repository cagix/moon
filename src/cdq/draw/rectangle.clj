(ns cdq.draw.rectangle
  (:require [cdq.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]))

(defn draw!
  [[_ x y w h color]
   {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/rectangle! shape-drawer x y w h))
