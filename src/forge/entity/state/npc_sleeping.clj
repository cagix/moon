(ns forge.entity.state.npc-sleeping
  (:require [anvil.body :as body]
            [anvil.faction :as faction]
            [anvil.fsm :as fsm]
            [anvil.graphics :refer [draw-text]]
            [anvil.stat :as stat]
            [anvil.string-effect :as string-effect]
            [anvil.world :as world :refer [nearest-entity-distance delayed-alert]]))

(defn ->v [[_ eid]]
  {:eid eid})

(defn exit [[_ {:keys [eid]}]]
  (delayed-alert (:position       @eid)
                 (:entity/faction @eid)
                 0.2)
  (swap! eid string-effect/add "[WHITE]!"))

(defn tick [_ eid]
  (let [entity @eid
        cell (get world/grid (body/tile entity))] ; pattern!
    (when-let [distance (nearest-entity-distance @cell (faction/enemy entity))]
      (when (<= distance (stat/->value entity :entity/aggro-range))
        (fsm/event eid :alert)))))

(defn render-above [_ entity]
  (let [[x y] (:position entity)]
    (draw-text {:text "zzz"
                :x x
                :y (+ y (:half-height entity))
                :up? true})))
