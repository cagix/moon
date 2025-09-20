(ns cdq.draw.circle
  (:require [com.badlogic.gdx.graphics.color :as color]
            [gdl.graphics.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer (color/create color))
  (sd/circle! shape-drawer x y radius))
