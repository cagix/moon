(ns cdq.application
  (:require [gdl.graphics :as graphics]
            [gdl.utils.disposable :as disp]))

(def state (atom nil))

(defn post-runnable! [runnable]
  (swap! state update-in [:ctx/app :gdl.app/runnables] conj runnable)
  nil)

(defn run-runnables! [{:keys [ctx/app] :as ctx}]
  (doseq [runnable (:gdl.app/runnables app)]
    (runnable ctx))
  (assoc-in ctx [:ctx/app :gdl.app/runnables] []))

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
