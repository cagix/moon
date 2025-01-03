(ns anvil.controls
  (:require [gdl.context :as c]))

(defn movement-vector [c]
  (c/WASD-movement-vector c))

(def help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")
