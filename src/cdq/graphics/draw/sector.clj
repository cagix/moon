(ns cdq.graphics.draw.sector
  (:require [com.badlogic.gdx.graphics.color :as color]
            [space.earlygrey.shape-drawer :as sd]
            [clojure.math-ext :refer [degree->radians]]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer (color/float-bits color))
  (sd/sector! shape-drawer
              center-x
              center-y
              radius
              (degree->radians start-angle)
              (degree->radians degree)))
