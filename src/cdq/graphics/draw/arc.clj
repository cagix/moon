(ns cdq.graphics.draw.arc
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.graphics.shape-drawer :as sd]
            [clojure.math-ext :refer [degree->radians]]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer (color/float-bits color))
  (sd/arc! shape-drawer
           center-x
           center-y
           radius
           (degree->radians start-angle)
           (degree->radians degree)))
