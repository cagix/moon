(ns cdq.ui.message
  (:require [cdq.ctx :as ctx]
            [clojure.gdx.ui :as ui]))

(defn- draw-message [state viewport]
  (when-let [text (:text @state)]
    [:draw/text {:x (/ (:width viewport) 2)
                 :y (+ (/ (:height viewport) 2) 200)
                 :text text
                 :scale 2.5
                 :up? true}]))

(defn create [_ctx]
  (ui/actor {:draw (fn [this {:keys [ctx/ui-viewport] :as ctx}]
                     (ctx/handle-draws! ctx
                                        [(draw-message (ui/user-object this)
                                                       ui-viewport)]))
             :act (fn [this delta _ctx]
                    (let [state (ui/user-object this)]
                      (when (:text @state)
                        (swap! state update :counter + delta)
                        (when (>= (:counter @state) 1.5)
                          (reset! state nil)))))
             :name "player-message"
             :user-object (atom nil)}))

(defn show! [message-actor text]
  (ui/set-user-object! message-actor (atom {:text text
                                            :counter 0})))
