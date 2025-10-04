(ns cdq.entity.temp-modifier.tick
  (:require [cdq.entity.stats :as stats]
            [clojure.timer :as timer]))

(defn txs
  [{:keys [modifiers counter]}
   eid
   {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc eid :entity/temp-modifier]
     [:tx/update eid :entity/stats stats/remove-mods modifiers]]))
