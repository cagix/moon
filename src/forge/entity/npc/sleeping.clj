(ns ^:no-doc forge.entity.npc.sleeping
  (:require [forge.entity.components :as entity]
            [forge.world :as world]))

(defn ->v [eid]
  {:eid eid})

(defn exit [{:keys [eid]}]
  (world/shout (:position @eid) (:entity/faction @eid) 0.2)
  (swap! eid entity/add-text-effect "[WHITE]!"))

(defn tick [_ eid]
  (let [entity @eid
        cell (world/cell (entity/tile entity))] ; pattern!
    (when-let [distance (world/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (entity/event eid :alert)))))
