(ns anvil.controls)

(defn unpaused? []
  (or (key-just-pressed? :p)
      (key-pressed? :space)))

(def close-windows-key :escape)

(def window-hotkeys
  {:inventory-window   :i
   :entity-info-window :e})

(defn movement-vector [])

(def help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")
