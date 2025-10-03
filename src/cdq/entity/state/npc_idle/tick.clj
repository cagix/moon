(ns cdq.entity.state.npc-idle.tick
  (:require [cdq.effect :as effect]
            [cdq.entity.body :as body]
            [cdq.entity.skills.skill :as skill]
            [cdq.world.grid :as grid]
            [cdq.world.raycaster :as raycaster]
            [cdq.world.potential-fields-movement :as potential-fields-movement]))

(defn- npc-effect-ctx
  [{:keys [world/grid]
    :as world}
   eid]
  (let [entity @eid
        target (grid/nearest-enemy grid entity)
        target (when (and target
                          (raycaster/line-of-sight? world entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (body/direction (:entity/body entity)
                                                (:entity/body @target)))}))

(defn- npc-choose-skill [world entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by :skill/cost)
       reverse
       (filter #(and (= :usable (skill/usable-state % entity effect-ctx))
                     (->> (:skill/effects %)
                          (filter (fn [e] (effect/applicable? e effect-ctx)))
                          (some (fn [e] (effect/useful? e effect-ctx world))))))
       first))

(defn txs [_ eid world]
  (let [effect-ctx (npc-effect-ctx world eid)]
    (if-let [skill (npc-choose-skill world @eid effect-ctx)]
      [[:tx/event eid :start-action [skill effect-ctx]]]
      [[:tx/event eid :movement-direction (or (potential-fields-movement/find-direction world eid)
                                              [0 0])]])))
