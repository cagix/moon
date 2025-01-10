(ns cdq.render
  (:require [gdl.context]
            [gdl.graphics]
            [gdl.graphics.camera :as cam]
            [cdq.context]
            [cdq.graphics]
            [cdq.graphics.tiled-map]))

(defn- set-camera-on-player! [{:keys [context/g
                                      cdq.context/player-eid]
                               :as context}]
  (cam/set-position! g (:position @player-eid))
  context)

; TODO select-keys use for each sub-system
; TODO handle-input! gets clojure.gdx/input, others not
; here no graphics,etc. ?
(defn game [context]
  (set-camera-on-player! context)
  (reduce (fn [context f]
            (f context))
          context
          [gdl.graphics/clear-screen
           cdq.graphics.tiled-map/render
           cdq.graphics/draw-world-view
           gdl.graphics/draw-stage

           ; updates
           gdl.context/update-stage
           cdq.context/handle-player-input
           cdq.context/update-mouseover-entity
           cdq.context/update-paused-state
           cdq.context/progress-time-if-not-paused
           cdq.context/remove-destroyed-entities  ; do not pause this as for example pickup item, should be destroyed.
           gdl.context/check-camera-controls
           cdq.context/check-ui-key-listeners]))
