(ns moon.screens.minimap
  (:require [gdl.graphics.camera :as cam]
            [gdl.graphics.color :as color]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.graphics.world-view :as world-view]
            [gdl.input :refer [key-just-pressed?]]
            [moon.graphics.tiled :as renderer]
            [moon.screen :as screen]
            [moon.world.tiled-map :refer [tiled-map explored-tile-corners]]))

; 28.4 viewportwidth
; 16 viewportheight
; camera shows :
;  [-viewportWidth/2, -(viewportHeight/2-1)] - [(viewportWidth/2-1), viewportHeight/2]
; zoom default '1'
; zoom 2 -> shows double amount

; we want min/max explored tiles X / Y and show the whole explored area....

(defn- minimap-zoom []
  (let [positions-explored (map first
                                (remove (fn [[position value]]
                                          (false? value))
                                        (seq @explored-tile-corners)))
        left   (apply min-key (fn [[x y]] x) positions-explored)
        top    (apply max-key (fn [[x y]] y) positions-explored)
        right  (apply max-key (fn [[x y]] x) positions-explored)
        bottom (apply min-key (fn [[x y]] y) positions-explored)]
    (cam/calculate-zoom (world-view/camera)
                       :left left
                       :top top
                       :right right
                       :bottom bottom)))

(defn- ->tile-corner-color-setter [explored?]
  (fn tile-corner-color-setter [color x y]
    (if (get explored? [x y]) color/white color/black)))

(deftype Minimap []
  screen/Screen
  (enter [_]
    (cam/set-zoom! (world-view/camera) (minimap-zoom)))

  (exit [_]
    (cam/reset-zoom! (world-view/camera)))

  (render [_]
    (renderer/draw tiled-map
                   (->tile-corner-color-setter @explored-tile-corners))
    (world-view/render (fn []
                         (sd/filled-circle (cam/position (world-view/camera)) 0.5 :green)))
    (when (or (key-just-pressed? :keys/tab)
              (key-just-pressed? :keys/escape))
      (screen/change :screens/world))))

(defn create []
  (->Minimap))
