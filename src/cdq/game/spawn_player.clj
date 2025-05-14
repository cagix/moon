(ns cdq.game.spawn-player
  (:require [cdq.ctx :as ctx]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.stage :as stage]
            [cdq.utils :as utils]
            [cdq.world :as world]))

(defn- player-entity-props [start-position]
  {:position (utils/tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor-key (state/cursor new-state-obj)]
                                                     (graphics/set-cursor! ctx/graphics cursor-key)))
                                 :skill-added! (fn [skill]
                                                 (stage/add-skill! ctx/stage skill))
                                 :skill-removed! (fn [skill]
                                                   (stage/remove-skill! ctx/stage skill))
                                 :item-set! (fn [inventory-cell item]
                                              (stage/set-item! ctx/stage inventory-cell item))
                                 :item-removed! (fn [inventory-cell]
                                                  (stage/remove-item! ctx/stage inventory-cell))}
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn do! []
  (utils/bind-root #'ctx/player-eid (world/spawn-creature (player-entity-props (:start-position ctx/world)))))
