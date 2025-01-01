(ns cdq.game-loop
  (:require [cdq.context :as context]
            [cdq.debug :as debug]
            [cdq.tile-color-setter :as tile-color-setter]
            [clojure.gdx :refer [clear-screen black]]
            [gdl.context :as c]
            [gdl.graphics.camera :as cam]
            [gdl.ui :as ui]))

(def ^:private ^:dbg-flag pausing? true)

(defn render [{:keys [gdl.context/world-viewport
                      cdq.context/tiled-map
                      cdq.context/player-eid
                      cdq.context/raycaster
                      cdq.context/explored-tile-corners]
               :as c}]
  (clear-screen black)
  ; FIXME position DRY
  (cam/set-position! (:camera world-viewport)
                     (:position @player-eid))
  ; FIXME position DRY
  (c/draw-tiled-map c
                    tiled-map
                    (tile-color-setter/create raycaster
                                              explored-tile-corners
                                              (cam/position (:camera world-viewport))))
  (c/draw-on-world-view c
                        (fn [c]
                          (debug/render-before-entities c)
                          ; FIXME position DRY (from player)
                          (context/render-entities c (map deref (context/active-entities c)))
                          (debug/render-after-entities c)))
  (let [stage (c/stage c)]
    (ui/draw stage c)
    (ui/act  stage c))
  (context/tick c pausing?))
