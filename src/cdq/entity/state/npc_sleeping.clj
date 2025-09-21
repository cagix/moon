(ns cdq.entity.state.npc-sleeping
  (:require [cdq.entity :as entity]))

(defn exit [_ eid _ctx]
  [[:tx/spawn-alert (entity/position @eid) (:entity/faction @eid) 0.2]
   [:tx/add-text-effect eid "[WHITE]!" 1]])
