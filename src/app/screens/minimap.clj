(ns ^:no-doc app.screens.minimap
  (:require [forge.graphics.camera :as cam]
            [forge.graphics.color :as color]
            [forge.input :refer [key-just-pressed?]]
            [forge.app :refer [draw-tiled-map draw-filled-circle draw-on-world-view world-camera change-screen]]
            [forge.screen :as screen]
            [moon.world :refer [tiled-map explored-tile-corners]]))

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
    (cam/calculate-zoom (world-camera)
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
    (cam/set-zoom! (world-camera) (minimap-zoom)))

  (exit [_]
    (cam/reset-zoom! (world-camera)))

  (render [_]
    (draw-tiled-map tiled-map
                    (->tile-corner-color-setter @explored-tile-corners))
    (draw-on-world-view
     (fn []
       (draw-filled-circle (cam/position (world-camera)) 0.5 :green)))
    (when (or (key-just-pressed? :keys/tab)
              (key-just-pressed? :keys/escape))
      (change-screen :screens/world)))

  (dispose [_]))

(defn create []
  {:screen (->Minimap)})
