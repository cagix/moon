(ns cdq.graphics.draw.filled-circle
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer (color/float-bits color))
  (sd/filled-circle! shape-drawer x y radius))
