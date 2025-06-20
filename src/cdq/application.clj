(ns cdq.application
  (:require [gdl.graphics :as graphics]
            [gdl.utils.disposable :as disp]))

(def state (atom nil))

; TODO call dispose! on all components
(defn dispose! []
  (let [{:keys [ctx/audio
                ctx/graphics
                ctx/world]} @state]
    (disp/dispose! audio)
    (disp/dispose! graphics)
    (disp/dispose! (:world/tiled-map world))
    ; TODO vis-ui dispose
    ; TODO what else disposable?
    ; => :ctx/tiled-map definitely and also dispose when re-creting gamestate.
    ))

; TODO call resize! on all components
(defn resize! [width height]
  (let [{:keys [ctx/graphics]} @state]
    (graphics/resize-viewports! graphics width height)))
