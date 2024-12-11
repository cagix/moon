(ns anvil.world.raycaster
  (:require [gdl.math.raycaster :as raycaster]))

(declare raycaster)

(defn ray-blocked? [start target]
  (raycaster/blocked? raycaster start target))

(defn path-blocked?
  "path-w in tiles. casts two rays."
  [start target path-w]
  (raycaster/path-blocked? raycaster start target path-w))
