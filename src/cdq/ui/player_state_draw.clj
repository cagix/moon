(ns cdq.ui.player-state-draw)

(def state->draw-gui-view)

(defn create [_ctx _params]
  {:actor/type :actor.type/actor
   :draw (fn [_this {:keys [ctx/player-eid]
                     :as ctx}]
           (when-let [f (state->draw-gui-view (:state (:entity/fsm @player-eid)))]
             (f player-eid ctx)))})
