(ns cdq.ui.player-state-draw
  (:require [cdq.graphics :as graphics]))

(defn create [{:keys [state->draw-gui-view]}]
  {:actor/type :actor.type/actor
   :draw (fn [_this {:keys [ctx/graphics
                            ctx/world] :as ctx}]
           (let [player-eid (:world/player-eid world)]
             (graphics/handle-draws! graphics
                                     (when-let [f (state->draw-gui-view (:state (:entity/fsm @player-eid)))]
                                       (f player-eid ctx)))))})
