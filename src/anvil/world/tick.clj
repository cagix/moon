(ns anvil.world.tick
  (:refer-clojure :exclude [time])
  (:require [anvil.world :as world]
            [gdl.graphics :as g]
            [gdl.stage :as stage]
            [gdl.utils :refer [defn-impl]]))

(defn player-input [])
(defn mouseover-entity [])
(defn paused-state [])
(defn time [])
(defn potential-fields [])
(defn entities [])
(defn remove-destroyed-entities [])
(defn camera-controls [camera])
(defn window-hotkeys  [stage])

(defn-impl world/tick []
  (player-input)
  (mouseover-entity)
  (paused-state)
  (when-not world/paused?
    (time)
    (potential-fields)
    (entities))
  (remove-destroyed-entities) ; do not pause this as for example pickup item, should be destroyed.
  (camera-controls g/camera)
  (window-hotkeys (stage/get)))
