(ns cdq.ctx.create.ui.message
  (:require [cdq.graphics :as graphics]
            [cdq.ui.message :as message]
            [clojure.gdx.viewport :as viewport])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(extend-type com.badlogic.gdx.scenes.scene2d.Actor
  message/Message
  (show! [this text]
    (Actor/.setUserObject this (atom {:text text
                                      :counter 0}))))

(defn- draw-message [state vp-width vp-height]
  (when-let [text (:text @state)]
    [:draw/text {:x (/ vp-width 2)
                 :y (+ (/ vp-height 2) 200)
                 :text text
                 :scale 2.5
                 :up? true}]))

(def duration-seconds 0.5)

(defn create [_ctx]
  (doto (proxy [Actor] []
          (draw [_batch _parent-alpha]
            (when-let [stage (.getStage this)]
              (graphics/draw! (:ctx/graphics (.ctx stage))
                              [(draw-message (.getUserObject this)
                                             (viewport/world-width  (.getViewport stage))
                                             (viewport/world-height (.getViewport stage)))])))
          (act [delta]
            (let [state (.getUserObject this)]
              (when (:text @state)
                (swap! state update :counter + delta)
                (when (>= (:counter @state) duration-seconds)
                  (reset! state nil))))))
    (.setName "player-message")
    (.setUserObject (atom nil))))
