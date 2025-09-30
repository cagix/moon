(ns cdq.ui.message
  (:require [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))


(defn show! [message-actor text]
  (actor/set-user-object! message-actor (atom {:text text
                                               :counter 0})))

(defn- draw-message [state vp-width vp-height]
  (when-let [text (:text @state)]
    [:draw/text {:x (/ vp-width 2)
                 :y (+ (/ vp-height 2) 200)
                 :text text
                 :scale 2.5
                 :up? true}]))

(def duration-seconds 0.5)

(defn create []
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
   :actor/name "player-message"
   :actor/user-object (atom nil)})
