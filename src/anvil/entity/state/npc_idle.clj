(ns ^:no-doc anvil.entity.state.npc-idle
  (:require [anvil.component :as component]
            [anvil.effect :as effect]
            [anvil.entity :as entity]
            [anvil.skill :as skill]
            [cdq.context :as world]
            [anvil.world.potential-field :as potential-field]))

(defn- effect-context [c eid]
  (let [entity @eid
        target (world/nearest-enemy entity)
        target (when (and target
                          (world/line-of-sight? c entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (effect/applicable-and-useful? ctx (:skill/effects %))))
       first))

(defmethods :npc-idle
  (component/->v [[_ eid] c]
    {:eid eid})

  (component/tick [_ eid c]
    (let [effect-ctx (effect-context c eid)]
      (if-let [skill (npc-choose-skill @eid effect-ctx)]
        (entity/event c eid :start-action [skill effect-ctx])
        (entity/event c eid :movement-direction (or (potential-field/find-direction c eid) [0 0]))))))
