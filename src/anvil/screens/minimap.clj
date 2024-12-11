(ns ^:no-doc anvil.screens.minimap
  (:require [gdl.graphics :as g]
            [gdl.graphics.camera :as cam]
            [gdl.input :refer [key-just-pressed?]]
            [anvil.level :as level :refer [explored-tile-corners]]
            [gdl.screen :as screen]))

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
    (cam/calculate-zoom g/camera
                        :left left
                        :top top
                        :right right
                        :bottom bottom)))

(defn- ->tile-corner-color-setter [explored?]
  (fn tile-corner-color-setter [color x y]
    (if (get explored? [x y]) g/white g/black)))

(deftype MinimapScreen []
  screen/Screen
  (enter [_]
    (cam/set-zoom! g/camera (minimap-zoom)))
  (exit [_]
    (cam/reset-zoom! g/camera))
  (dispose [_])
  (render [_]
    (g/draw-tiled-map level/tiled-map
                      (->tile-corner-color-setter @explored-tile-corners))
    (g/draw-on-world-view
     (fn []
       (g/filled-circle (cam/position g/camera) 0.5 :green)))
    (when (or (key-just-pressed? :keys/tab)
              (key-just-pressed? :keys/escape))
      (screen/change :screens/world))))

(defn screen []
  (->MinimapScreen))
