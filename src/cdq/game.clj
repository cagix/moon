(ns cdq.game
  (:require [cdq.context :refer [active-entities max-delta-time check-player-input set-camera-on-player-position render-tiled-map update-mouseover-entity update-time tick-potential-fields update-paused-state render-entities tick-entities remove-destroyed-entities check-ui-key-listeners]]
            [cdq.debug :as debug]
            [clojure.gdx :refer [clear-screen black]]
            [gdl.context :as c]
            [gdl.ui :as ui]))

(def ^:private ^:dbg-flag pausing? true)

(defn process-frame [c]
  (clear-screen black)
  ; FIXME position DRY
  (set-camera-on-player-position c)
  ; FIXME position DRY
  (render-tiled-map c)
  ; render/entities
  (c/draw-on-world-view c
                        (fn [c]
                          (debug/render-before-entities c)
                          ; FIXME position DRY (from player)
                          (render-entities c)
                          (debug/render-after-entities c)))
  (let [stage (c/stage c)]
    (ui/draw stage c)
    (ui/act  stage c))
  (check-player-input c)
  (let [c (-> c
              update-mouseover-entity
              (update-paused-state pausing?))
        c (if (:cdq.context/paused? c)
            c
            (-> c
                update-time
                tick-potential-fields
                tick-entities))]
    (remove-destroyed-entities c) ; do not pause this as for example pickup item, should be destroyed.
    (c/check-camera-controls c)
    (check-ui-key-listeners c)
    c))
