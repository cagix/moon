(ns cdq.ui.message
  (:require [cdq.g :as g]
            [gdl.ui :as ui]))

(defn- draw-message [state ctx]
  (when-let [text (:text @state)]
    [:draw/text {:x (/ (g/ui-viewport-width ctx) 2)
                 :y (+ (/ (g/ui-viewport-height ctx) 2) 200)
                 :text text
                 :scale 2.5
                 :up? true}]))

(defn create [& {:keys [name]}]
  (ui/actor {:draw (fn [this ctx]
                     (g/handle-draws! ctx [(draw-message (ui/user-object this) ctx)]))
             :act (fn [this delta _ctx]
                    (let [state (ui/user-object this)]
                      (when (:text @state)
                        (swap! state update :counter + delta)
                        (when (>= (:counter @state) 1.5)
                          (reset! state nil)))))
             :name name
             :user-object (atom nil)}))

(defn show! [message-actor text]
  (ui/set-user-object! message-actor (atom {:text text
                                            :counter 0})))
