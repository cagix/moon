(ns cdq.ui.player-state-draw
  (:require [cdq.ctx.graphics :as graphics]
            [cdq.entity.state.player-item-on-cursor]))

(def state->draw-gui-view
  {:player-item-on-cursor (fn
                            [eid
                             {:keys [ctx/graphics
                                     ctx/mouseover-actor
                                     ctx/ui-mouse-position]}]
                            (when (not (cdq.entity.state.player-item-on-cursor/world-item? mouseover-actor))
                              [[:draw/texture-region
                                (graphics/texture-region graphics (:entity/image (:entity/item-on-cursor @eid)))
                                ui-mouse-position
                                {:center? true}]]))})

(defn create [_ctx _params]
  {:actor/type :actor.type/actor
   :draw (fn [_this {:keys [ctx/world]
                     :as ctx}]
           (let [player-eid (:world/player-eid world)]
             (when-let [f (state->draw-gui-view (:state (:entity/fsm @player-eid)))]
               (f player-eid ctx))))})
