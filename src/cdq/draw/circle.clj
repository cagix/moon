(ns cdq.draw.circle
  (:require [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]))

(defn do!
  [{:keys [ctx/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/circle! shape-drawer x y radius))
