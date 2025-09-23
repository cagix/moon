(ns cdq.create.graphics.draw.arc
  (:require [clojure.utils :as utils]
            [clojure.graphics.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer
           center-x
           center-y
           radius
           (utils/degree->radians start-angle)
           (utils/degree->radians degree)))
