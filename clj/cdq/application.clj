(ns cdq.application
  (:require [gdl.app]
            [gdl.graphics :as graphics]
            [gdl.malli :as m])
  (:import (com.badlogic.gdx.utils Disposable)))

(def state (atom nil))

(defn post-runnable! [runnable]
  (swap! state gdl.app/add-runnable runnable)
  nil)

; TODO call dispose! on all components
(defn dispose! []
  (let [{:keys [ctx/audio
                ctx/graphics
                ctx/world]} @state]
    (Disposable/.dispose audio)
    (Disposable/.dispose graphics)
    (Disposable/.dispose (:world/tiled-map world))
    ; TODO vis-ui dispose
    ; TODO what else disposable?
    ; => :ctx/tiled-map definitely and also dispose when re-creting gamestate.
    ))

; TODO call resize! on all components
(defn resize! [width height]
  (let [{:keys [ctx/graphics]} @state]
    (graphics/resize-viewports! graphics width height)))
