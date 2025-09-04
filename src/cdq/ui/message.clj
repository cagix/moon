(ns cdq.ui.message
  (:require [cdq.ui.actor :as actor]))

(defn- draw-message [state viewport]
  (when-let [text (:text @state)]
    [:draw/text {:x (/ (:viewport/width viewport) 2)
                 :y (+ (/ (:viewport/height viewport) 2) 200)
                 :text text
                 :scale 2.5
                 :up? true}]))

(defn- create*
  [{:keys [duration-seconds
           name]}]
  {:actor/type :actor.type/actor
   :draw (fn [this {:keys [ctx/graphics]}]
           [(draw-message (actor/user-object this)
                          (:g/ui-viewport graphics))])
   :act (fn [this delta _ctx]
          (let [state (actor/user-object this)]
            (when (:text @state)
              (swap! state update :counter + delta)
              (when (>= (:counter @state) duration-seconds)
                (reset! state nil)))))
   :name name
   :user-object (atom nil)})

(defn show! [message-actor text]
  (actor/set-user-object! message-actor (atom {:text text
                                               :counter 0})))

(defn create [ctx]
  (create* {:duration-seconds 0.5
            :name "player-message"}))
