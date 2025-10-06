(ns cdq.graphics.draw.circle
  (:require [com.badlogic.gdx.graphics.color :as color]
            [clojure.graphics.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer (color/float-bits color))
  (sd/circle! shape-drawer x y radius))
