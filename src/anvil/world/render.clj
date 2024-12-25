(ns anvil.world.render
  (:require [gdl.context :as c]
            [gdl.graphics.camera :as cam]
            [anvil.world :as world]))

(defn tiled-map [tiled-map light-position])

(defn debug-before-entities [c])

(defn entities [c entities])

(defn debug-after-entities [c])

(defn-impl world/render []
  ; FIXME position DRY
  (cam/set-position! c/camera (:position @world/player-eid))
  ; FIXME position DRY
  (tiled-map world/tiled-map (cam/position c/camera))
  (c/draw-on-world-view (c/get-ctx)
                        (fn [c]
                          (debug-before-entities c)
                          ; FIXME position DRY (from player)
                          (entities c (map deref (world/active-entities)))
                          (debug-after-entities c))))
