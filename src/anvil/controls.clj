(ns anvil.controls)

(defn unpaused? []
  (or (key-just-pressed? :keys/p)
      (key-pressed? :keys/space)))

(def close-windows-key :keys/escape)

(def window-hotkeys
  {:inventory-window   :keys/i
   :entity-info-window :keys/e})

(defn movement-vector [])

(def help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")
