(ns cdq.entity.state.npc-idle
  (:require [cdq.world :as world]
            [cdq.creature :as creature]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.gdx.math.vector2 :as v]
            [cdq.world.grid :as grid]))

(defn- npc-choose-skill [ctx entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (creature/skill-usable-state entity % effect-ctx))
                     (effect/applicable-and-useful? ctx effect-ctx (:skill/effects %))))
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

(defn tick! [_ eid {:keys [ctx/world] :as ctx}]
  (let [effect-ctx (npc-effect-ctx world eid)]
    (if-let [skill (npc-choose-skill ctx @eid effect-ctx)]
      [[:tx/event eid :start-action [skill effect-ctx]]]
      [[:tx/event eid :movement-direction (or (world/find-movement-direction world eid)
                                              [0 0])]])))
