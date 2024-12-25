(ns anvil.world.tick
  (:refer-clojure :exclude [time])
  (:require [anvil.controls :as controls]
            [anvil.world :as world]
            [gdl.context :as ctx]
            [gdl.stage :as stage]))

(defn player-input [])
(defn mouseover-entity [c])
(defn paused-state [])
(defn time [])
(defn potential-fields [])
(defn entities [])
(defn remove-destroyed-entities [])
(defn camera-controls [camera])
(defn window-hotkeys  [stage])

(defn-impl world/tick [c pausing?]
  (player-input)
  (mouseover-entity c)
  (paused-state pausing?)
  (when-not world/paused?
    (time)
    (potential-fields)
    (entities))
  (remove-destroyed-entities) ; do not pause this as for example pickup item, should be destroyed.
  (camera-controls ctx/camera)
  (window-hotkeys {:controls/close-windows-key controls/close-windows-key
                   :controls/window-hotkeys    controls/window-hotkeys}
                  (stage/get)))
