(ns cdq.ui.message
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [gdl.ui :as ui]))

(defn create [& {:keys [name]}]
  (ui/actor {:draw (fn [this]
                     (let [draw (ctx/get-draw)
                           state (ui/user-object this)]
                       (when-let [text (:text @state)]
                         (draw/text draw
                                    {:x (/ (:width     ctx/ui-viewport) 2)
                                     :y (+ (/ (:height ctx/ui-viewport) 2) 200)
                                     :text text
                                     :scale 2.5
                                     :up? true}))))
             :act (fn [this delta]
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
