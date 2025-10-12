(ns cdq.graphics.draw.circle
  (:require [clojure.color :as color]
            [clojure.gdx.graphics.g2d.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer (color/float-bits color))
  (sd/circle! shape-drawer x y radius))
