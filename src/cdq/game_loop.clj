(ns cdq.game-loop
  (:require [cdq.context :as context]
            [clojure.gdx :refer [clear-screen black]]
            [gdl.app :as app]
            [gdl.context :as c]
            [gdl.graphics.camera :as cam]
            [gdl.ui :as ui]))

(def ^:private ^:dbg-flag pausing? true)

(defn render [{:keys [gdl.context/world-viewport
                      cdq.context/tiled-map
                      cdq.context/player-eid]
               :as c}]
  (clear-screen black)
  ; FIXME position DRY
  (cam/set-position! (:camera world-viewport)
                     (:position @player-eid))
  ; FIXME position DRY
  (context/render-tiled-map c tiled-map (cam/position (:camera world-viewport)))
  (c/draw-on-world-view c
                        (fn [c]
                          (context/render-debug-before-entities c)
                          ; FIXME position DRY (from player)
                          (context/render-entities c (map deref (context/active-entities c)))
                          (context/render-debug-after-entities c)))
  (let [stage (c/stage c)]
    (ui/draw stage c)
    (ui/act  stage c))
  (context/tick c pausing?))
