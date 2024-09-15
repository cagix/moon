(ns components.screens.minimap
  (:require [gdx.graphics.orthographic-camera :as orthographic-camera]
            [gdx.graphics.camera :as camera]
            [gdx.input :as input]
            utils.camera
            [core.component :refer [defcomponent] :as component]
            [core.context :as ctx :refer [explored?]]
            [core.graphics :as g])
  (:import com.badlogic.gdx.Input$Keys
           com.badlogic.gdx.graphics.Color))

; 28.4 viewportwidth
; 16 viewportheight
; camera shows :
;  [-viewportWidth/2, -(viewportHeight/2-1)] - [(viewportWidth/2-1), viewportHeight/2]
; zoom default '1'
; zoom 2 -> shows double amount

; we want min/max explored tiles X / Y and show the whole explored area....

(defn- calculate-zoom [{:keys [world/explored-tile-corners] :as ctx}]
  (let [positions-explored (map first
                                (remove (fn [[position value]]
                                          (false? value))
                                        (seq @explored-tile-corners)))
        left   (apply min-key (fn [[x y]] x) positions-explored)
        top    (apply max-key (fn [[x y]] y) positions-explored)
        right  (apply max-key (fn [[x y]] x) positions-explored)
        bottom (apply min-key (fn [[x y]] y) positions-explored)]
    (utils.camera/calculate-zoom (ctx/world-camera ctx)
                                 :left left
                                 :top top
                                 :right right
                                 :bottom bottom)))

(defn- ->tile-corner-color-setter [explored?]
  (fn tile-corner-color-setter [color x y]
    (if (get explored? [x y])
      Color/WHITE
      Color/BLACK)))

#_(deftype Screen []
  (show [_ ctx]
    (orthographic-camera/set-zoom! (ctx/world-camera ctx) (calculate-zoom ctx)))

  (hide [_ ctx]
    (orthographic-camera/reset-zoom! (ctx/world-camera ctx)))

  ; TODO fixme not subscreen
  (render [_ {:keys [world/tiled-map world/explored-tile-corners] :as context}]
    (ctx/render-tiled-map context
                          tiled-map
                          (->tile-corner-color-setter @explored-tile-corners))
    (ctx/render-world-view context
                           (fn [g]
                             (g/draw-filled-circle g
                                                   (camera/position (ctx/world-camera context))
                                                   0.5
                                                   Color/GREEN)))
    (if (or (input/key-just-pressed? Input$Keys/TAB)
            (input/key-just-pressed? Input$Keys/ESCAPE))
      (ctx/change-screen context :screens/world)
      context)))

#_(defcomponent :screens/minimap
  (component/create [_ _ctx]
    (->Screen)))
