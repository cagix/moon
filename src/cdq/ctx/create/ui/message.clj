(ns cdq.ctx.create.ui.message
  (:require [cdq.ui.message :as message]
            [clojure.graphics.viewport :as viewport]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.stage :as stage])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(extend-type Actor
  message/Message
  (show! [this text]
    (actor/set-user-object! this (atom {:text text
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
                 [(draw-message (actor/user-object this)
                                (viewport/world-width  (stage/viewport (actor/get-stage this)))
                                (viewport/world-height (stage/viewport (actor/get-stage this))))])
   :actor/act (fn [this delta _ctx]
                (let [state (actor/user-object this)]
                  (when (:text @state)
                    (swap! state update :counter + delta)
                    (when (>= (:counter @state) duration-seconds)
                      (reset! state nil)))))
   :actor/name "player-message"
   :actor/user-object (atom nil)})
