(ns ^:no-doc forge.screens.minimap
  (:require [anvil.graphics :as g]
            [anvil.graphics.camera :as cam]
            [anvil.level :as level :refer [explored-tile-corners]]
            [anvil.screen :as screen]
            [anvil.world :as world]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.input :refer [key-just-pressed?]]))

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
    (cam/calculate-zoom (world/camera)
                        :left left
                        :top top
                        :right right
                        :bottom bottom)))

(defn- ->tile-corner-color-setter [explored?]
  (fn tile-corner-color-setter [color x y]
    (if (get explored? [x y]) color/white color/black)))

(defn enter []
  (cam/set-zoom! (world/camera) (minimap-zoom)))

(defn exit []
  (cam/reset-zoom! (world/camera)))

(defn render []
  (world/draw-tiled-map level/tiled-map
                        (->tile-corner-color-setter @explored-tile-corners))
  (world/draw-on-view
   (fn []
     (g/filled-circle (cam/position (world/camera)) 0.5 :green)))
  (when (or (key-just-pressed? :keys/tab)
            (key-just-pressed? :keys/escape))
    (screen/change :screens/world)))
