(ns cdq.game.spawn-player
  (:require [cdq.ctx :as ctx]
            [cdq.stage :as stage]
            [cdq.state :as state]
            [cdq.utils :as utils]))

(defn- player-entity-props [start-position]
  {:position (utils/tile->middle start-position)
   :creature-id (:creature-id ctx/player-entity-config)
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor (state/cursor new-state-obj)]
                                                     [[:tx/set-cursor cursor]]))
                                 :skill-added! (fn [skill]
                                                 (stage/add-skill! ctx/stage skill))
                                 :skill-removed! (fn [skill]
                                                   (stage/remove-skill! ctx/stage skill))
                                 :item-set! (fn [inventory-cell item]
                                              (stage/set-item! ctx/stage inventory-cell item))
                                 :item-removed! (fn [inventory-cell]
                                                  (stage/remove-item! ctx/stage inventory-cell))}
                :entity/free-skill-points (:free-skill-points ctx/player-entity-config)
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles (:click-distance-tiles ctx/player-entity-config)}})

(defn do! []
  (utils/handle-txs! [[:tx/spawn-creature (player-entity-props (:start-position ctx/world))]]))
