(ns cdq.application
  (:require [cdq.app]
            [cdq.application.context]
            [cdq.core]
            [cdq.graphics :as graphics]
            [cdq.malli :as m])
  (:import (com.badlogic.gdx.utils Disposable)))

(def state (atom nil))

(defn post-runnable! [runnable]
  (swap! state cdq.app/add-runnable runnable)
  nil)

(defn create! [context create-fns]
  (reset! state (reduce cdq.core/render*
                        (cdq.application.context/create context)
                        create-fns)))

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

(defn render! [render-fns]
  (swap! state (fn [ctx]
                 (reduce cdq.core/render* ctx render-fns))))
