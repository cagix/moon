(ns cdq.draw.arc
  (:require [cdq.utils :as utils]
            [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]))

(defn do!
  [{:keys [ctx/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/arc! shape-drawer
           center-x
           center-y
           radius
           (utils/degree->radians start-angle)
           (utils/degree->radians degree)))
