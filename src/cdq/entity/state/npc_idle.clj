(ns cdq.entity.state.npc-idle
  (:require [cdq.effect-context :as effect-ctx]
            [anvil.entity :as entity]
            [anvil.skill :as skill]
            [anvil.world.potential-field :as potential-field]
            [cdq.context :as world]))

(defn- npc-choose-skill [c entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (effect-ctx/applicable-and-useful? c ctx (:skill/effects %))))
       first))

(defn- effect-context [c eid]
  (let [entity @eid
        target (world/nearest-enemy c entity)
        target (when (and target
                          (world/line-of-sight? c entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defn create [[_ eid] c]
  {:eid eid})

(defn tick [_ eid c]
  (let [effect-ctx (effect-context c eid)]
    (if-let [skill (npc-choose-skill c @eid effect-ctx)]
      (entity/event c eid :start-action [skill effect-ctx])
      (entity/event c eid :movement-direction (or (potential-field/find-direction c eid) [0 0])))))
