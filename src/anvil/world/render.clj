(ns anvil.world.render
  (:require [gdl.graphics :as g]
            [gdl.graphics.camera :as cam]
            [anvil.world :as world]
            [gdl.utils :refer [defn-impl]]))

(defn tiled-map [tiled-map light-position])

(defn debug-before-entities [])

(defn entities [entities])

(defn debug-after-entities [])

(defn-impl world/render []
  ; FIXME position DRY
  (cam/set-position! g/camera (:position @world/player-eid))
  ; FIXME position DRY
  (tiled-map world/tiled-map (cam/position g/camera))
  (g/draw-on-world-view (fn []
                          (debug-before-entities)
                          ; FIXME position DRY (from player)
                          (entities (map deref (world/active-entities)))
                          (debug-after-entities))))
