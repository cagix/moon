(ns cdq.entity.state.npc-sleeping
  (:require [cdq.entity :as entity]
            [cdq.context :as world :refer [add-text-effect]]
            [cdq.grid :as grid]))

(defn create [[_ eid] c]
  {:eid eid})

(defn exit [[_ {:keys [eid]}] c]
  (world/delayed-alert c
                       (:position       @eid)
                       (:entity/faction @eid)
                       0.2)
  (swap! eid add-text-effect c "[WHITE]!"))

(defn tick [_ eid c]
  (let [entity @eid
        cell (world/grid-cell c (entity/tile entity))] ; pattern!
    (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (world/send-event! c eid :alert)))))
