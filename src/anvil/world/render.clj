(ns anvil.world.render
  (:require [gdl.context :as c]
            [gdl.graphics.camera :as cam]
            [cdq.context :as world]))

(defn render-tiled-map [c tiled-map light-position])

(defn debug-before-entities [c])

(defn entities [c entities])

(defn debug-after-entities [c])

(defn-impl world/render [{:keys [gdl.context/world-viewport
                                 cdq.context/tiled-map
                                 cdq.context/player-eid]
                          :as c}]
  ; FIXME position DRY
  (cam/set-position! (:camera world-viewport)
                     (:position @player-eid))
  ; FIXME position DRY
  (render-tiled-map c tiled-map (cam/position (:camera world-viewport)))
  (c/draw-on-world-view c
                        (fn [c]
                          (debug-before-entities c)
                          ; FIXME position DRY (from player)
                          (entities c (map deref (world/active-entities)))
                          (debug-after-entities c))))
