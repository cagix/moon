(ns cdq.ctx.spawn-enemies
  (:require [cdq.utils :as utils]
            [gdl.tiled :as tiled]))

(defn- spawn-enemies [tiled-map]
  (for [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                {:position position
                 :creature-id (keyword creature-id)
                 :components {:entity/fsm {:fsm :fsms/npc
                                           :initial-state :npc-sleeping}
                              :entity/faction :evil}})]
    [:tx/spawn-creature (update props :position utils/tile->middle)]))

(defn do! [{:keys [ctx/tiled-map]}]
  (utils/handle-txs! (spawn-enemies tiled-map)))
