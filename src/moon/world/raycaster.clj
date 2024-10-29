(ns moon.world.raycaster
  (:require [gdl.math.raycaster :as raycaster]))

(declare ^:private raycaster)

(defn init [grid blocks-vision?]
  (bind-root #'raycaster (raycaster/create grid blocks-vision?)))

(defn ray-blocked? [start target]
  (raycaster/blocked? raycaster start target))

(defn path-blocked?
  "path-w in tiles. casts two rays."
  [start target path-w]
  (raycaster/path-blocked? raycaster start target path-w))
