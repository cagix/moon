(ns anvil.entity.state.npc-idle
  (:require [anvil.component :as component]
            [anvil.effect :as effect]
            [anvil.entity.body :as body]
            [anvil.entity.fsm :as fsm]
            [anvil.skill :as skill]
            [anvil.world :as world]
            [anvil.world.potential-field :as potential-field]
            [gdl.utils :refer [defmethods]]))

(defn- effect-context [eid]
  (let [entity @eid
        target (world/nearest-enemy entity)
        target (when (and target
                          (world/line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (body/direction entity @target))}))

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
  (component/->v [[_ eid]]
    {:eid eid})

  (component/tick [_ eid]
    (let [effect-ctx (effect-context eid)]
      (if-let [skill (npc-choose-skill @eid effect-ctx)]
        (fsm/event eid :start-action [skill effect-ctx])
        (fsm/event eid :movement-direction (or (potential-field/find-direction eid) [0 0]))))))
