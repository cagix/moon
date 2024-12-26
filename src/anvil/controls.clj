(ns anvil.controls
  (:require [clojure.gdx :refer [key-pressed? key-just-pressed?]]))

(defn unpaused? [c]
  (or (key-just-pressed? c :p)
      (key-pressed? c :space)))

(def close-windows-key :escape)

(def window-hotkeys
  {:inventory-window   :i
   :entity-info-window :e})

(defn movement-vector [])

(def help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")
