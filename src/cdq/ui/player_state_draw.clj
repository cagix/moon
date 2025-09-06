(ns cdq.ui.player-state-draw
  (:require [cdq.image :as image]
            [cdq.entity.state.player-item-on-cursor]))

(def state->draw-gui-view
  {:player-item-on-cursor (fn
                            [eid
                             {:keys [ctx/textures
                                     ctx/mouseover-actor
                                     ctx/ui-mouse-position]}]
                            (when (not (cdq.entity.state.player-item-on-cursor/world-item? mouseover-actor))
                              [[:draw/texture-region
                                (image/texture-region (:entity/image (:entity/item-on-cursor @eid))
                                                      textures)
                                ui-mouse-position
                                {:center? true}]]))})

(defn create [_ctx _params]
  {:actor/type :actor.type/actor
   :draw (fn [_this {:keys [ctx/player-eid]
                     :as ctx}]
           (when-let [f (state->draw-gui-view (:state (:entity/fsm @player-eid)))]
             (f player-eid ctx)))})
