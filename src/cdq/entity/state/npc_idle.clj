(ns cdq.entity.state.npc-idle
  (:require [cdq.creature :as creature]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.world :as world]
            [cdq.world.grid :as grid]
            [gdl.math.vector2 :as v]))

(def movement-ai-logic (requiring-resolve 'cdq.potential-fields.movement/find-movement-direction))

(defn- find-movement-direction [{:keys [world/grid]} eid]
  (movement-ai-logic grid eid))

(defn- npc-choose-skill [world entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (creature/skill-usable-state entity % effect-ctx))
                     (->> (:skill/effects %)
                          (filter (fn [e] (effect/applicable? e effect-ctx)))
                          (some (fn [e] (effect/useful? e effect-ctx world))))))
       first))

(defn- npc-effect-ctx
  [{:keys [world/grid]
    :as world}
   eid]
  (let [entity @eid
        target (grid/nearest-enemy grid entity)
        target (when (and target
                          (world/line-of-sight? world entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (v/direction (entity/position entity)
                                             (entity/position @target)))}))

(defn tick [_ eid world]
  (let [effect-ctx (npc-effect-ctx world eid)]
    (if-let [skill (npc-choose-skill world @eid effect-ctx)]
      [[:tx/event eid :start-action [skill effect-ctx]]]
      [[:tx/event eid :movement-direction (or (find-movement-direction world eid)
                                              [0 0])]])))
