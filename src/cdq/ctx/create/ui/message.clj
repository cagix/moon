(ns cdq.ctx.create.ui.message
  (:require [cdq.ui.message :as message]
            [clojure.gdx.viewport :as viewport]
            [cdq.ui.stage :as stage])
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
  {:actor/type :actor.type/actor
   :actor/draw (fn [this _ctx]
                 [(draw-message (Actor/.getUserObject this)
                                (viewport/world-width  (stage/viewport (Actor/.getStage this)))
                                (viewport/world-height (stage/viewport (Actor/.getStage this))))])
   :actor/act (fn [this delta _ctx]
                (let [state (Actor/.getUserObject this)]
                  (when (:text @state)
                    (swap! state update :counter + delta)
                    (when (>= (:counter @state) duration-seconds)
                      (reset! state nil)))))
   :actor/name "player-message"
   :actor/user-object (atom nil)})
