(ns ^:no-doc forge.screens.minimap
  (:require [forge.core :refer :all]
            [forge.world :refer [tiled-map explored-tile-corners]]))

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
    (calculate-zoom (world-camera)
                    :left left
                    :top top
                    :right right
                    :bottom bottom)))

(defn- ->tile-corner-color-setter [explored?]
  (fn tile-corner-color-setter [color x y]
    (if (get explored? [x y]) white black)))

(deftype Minimap []
  Screen
  (screen-enter [_]
    (set-zoom! (world-camera) (minimap-zoom)))

  (screen-exit [_]
    (reset-zoom! (world-camera)))

  (screen-render [_]
    (draw-tiled-map tiled-map
                    (->tile-corner-color-setter @explored-tile-corners))
    (draw-on-world-view
     (fn []
       (draw-filled-circle (cam-position (world-camera)) 0.5 :green)))
    (when (or (key-just-pressed? :keys/tab)
              (key-just-pressed? :keys/escape))
      (change-screen :screens/world)))

  (screen-destroy [_]))

(defn create []
  {:screen (->Minimap)})
