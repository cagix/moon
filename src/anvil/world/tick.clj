(ns anvil.world.tick
  (:refer-clojure :exclude [time])
  (:require [anvil.controls :as controls]
            [cdq.context :as world]
            [gdl.context :as ctx]
            [gdl.stage :as stage]))

(defn player-input [c])
(defn mouseover-entity [c])
(defn paused-state [c])
(defn time [])
(defn potential-fields [c])
(defn entities [c])
(defn remove-destroyed-entities [c])
(defn camera-controls [camera])
(defn window-hotkeys  [stage])

(defn-impl world/tick [{:keys [gdl.context/world-viewport] :as c} pausing?]
  (player-input c)
  (mouseover-entity c)
  (paused-state c pausing?)
  (when-not world/paused?
    (time)
    (potential-fields c)
    (entities c))
  (remove-destroyed-entities c) ; do not pause this as for example pickup item, should be destroyed.
  (camera-controls (:camera world-viewport))
  (window-hotkeys {:controls/close-windows-key controls/close-windows-key
                   :controls/window-hotkeys    controls/window-hotkeys}
                  (stage/get)))
