(ns cdq.draw.arc
  (:require [clojure.utils :as utils]
            [space.earlygrey.shape-drawer :as sd]))

(defn do!
  [{:keys [ctx/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer
           center-x
           center-y
           radius
           (utils/degree->radians start-angle)
           (utils/degree->radians degree)))
