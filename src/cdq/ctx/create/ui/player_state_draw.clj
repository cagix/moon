(ns cdq.ctx.create.ui.player-state-draw)

(def state->draw-ui-view
  (update-vals {:player-item-on-cursor 'cdq.ctx.create.ui.player-state-draw.player-item-on-cursor/draws}
               requiring-resolve))

(defn create [_ctx]
  {:actor/type :actor.type/actor
   :actor/draw (fn [_this {:keys [ctx/world]
                           :as ctx}]
                 (let [player-eid (:world/player-eid world)
                       entity @player-eid
                       state-k (:state (:entity/fsm entity))]
                   (when-let [f (state->draw-ui-view state-k)]
                     (f player-eid ctx))))})
