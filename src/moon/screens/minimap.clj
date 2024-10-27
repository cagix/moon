(ns ^:no-doc moon.screens.minimap)

; 28.4 viewportwidth
; 16 viewportheight
; camera shows :
;  [-viewportWidth/2, -(viewportHeight/2-1)] - [(viewportWidth/2-1), viewportHeight/2]
; zoom default '1'
; zoom 2 -> shows double amount

; we want min/max explored tiles X / Y and show the whole explored area....

#_(defn- minimap-zoom []
  (let [positions-explored (map first
                                (remove (fn [[position value]]
                                          (false? value))
                                        (seq @world/explored-tile-corners)))
        left   (apply min-key (fn [[x y]] x) positions-explored)
        top    (apply max-key (fn [[x y]] y) positions-explored)
        right  (apply max-key (fn [[x y]] x) positions-explored)
        bottom (apply min-key (fn [[x y]] y) positions-explored)]
    (cam/calculate-zoom (world-view/camera)
                       :left left
                       :top top
                       :right right
                       :bottom bottom)))

#_(defn- ->tile-corner-color-setter [explored?]
  (fn tile-corner-color-setter [color x y]
    (if (get explored? [x y]) color/white color/black)))

#_(deftype Screen []
    (show [_]
      (cam/set-zoom! (world-view/camera) (minimap-zoom)))

    (hide [_]
      (cam/reset-zoom! (world-view/camera)))

    ; TODO fixme not subscreen
    (render [_]
      (tiled-map-renderer/draw world/tiled-map
                               (->tile-corner-color-setter @world/explored-tile-corners))
      (g/render-world-view! (fn []
                              (sd/filled-circle (cam/camera-position (world-view/camera))
                                                0.5
                                                :green)))
      (when (or (key-just-pressed? :keys/tab)
                (key-just-pressed? :keys/escape))
        (screen/change :screens/world))))

#_(defc :screens/minimap
  (component/create [_]
    (->Screen)))
