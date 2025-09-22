(ns cdq.create.ui.message
  (:require [cdq.ui.message]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]))

(extend-type com.badlogic.gdx.scenes.scene2d.Actor
  cdq.ui.message/Message
  (show! [message-actor text]
    (actor/set-user-object! message-actor (atom {:text text
                                                 :counter 0}))))

(defn- draw-message [state vp-width vp-height]
  (when-let [text (:text @state)]
    [:draw/text {:x (/ vp-width 2)
                 :y (+ (/ vp-height 2) 200)
                 :text text
                 :scale 2.5
                 :up? true}]))

(defn create
  [_ctx
   {:keys [duration-seconds
           name]}]
  {:actor/type :actor.type/actor
   :draw (fn [this {:keys [ctx/stage]}]
           [(draw-message (actor/user-object this)
                          (:viewport/width  (stage/viewport stage))
                          (:viewport/height (stage/viewport stage)))])
   :act (fn [this delta _ctx]
          (let [state (actor/user-object this)]
            (when (:text @state)
              (swap! state update :counter + delta)
              (when (>= (:counter @state) duration-seconds)
                (reset! state nil)))))
   :actor/name name
   :actor/user-object (atom nil)})
