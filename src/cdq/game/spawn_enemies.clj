(ns cdq.game.spawn-enemies
  (:require [cdq.ctx :as ctx]
            [cdq.tiled :as tiled]
            [cdq.utils :as utils]))

(defn do! []
  (utils/handle-txs!
   (for [props (for [[position creature-id] (tiled/positions-with-property (:tiled-map ctx/world) :creatures :id)]
                 {:position position
                  :creature-id (keyword creature-id)
                  :components {:entity/fsm {:fsm :fsms/npc
                                            :initial-state :npc-sleeping}
                               :entity/faction :evil}})]
     [:tx/spawn-creature (update props :position utils/tile->middle)])))
