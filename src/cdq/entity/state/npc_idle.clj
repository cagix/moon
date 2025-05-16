(ns cdq.entity.state.npc-idle
  (:require [cdq.ctx :as ctx]
            [cdq.cell :as cell]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.potential-field :as potential-field]
            [cdq.utils :refer [defcomponent]]))

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (entity/skill-usable-state entity % ctx))
                     (effect/applicable-and-useful? ctx (:skill/effects %))))
       first))

(defn- npc-effect-context [eid]
  (let [entity @eid
        target (cell/nearest-entity @(ctx/grid (entity/tile entity))
                                    (entity/enemy entity))
        target (when (and target
                          (entity/line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defcomponent :npc-idle
  (entity/create [[_ eid]]
    {:eid eid})

  (entity/tick! [_ eid]
    (let [effect-ctx (npc-effect-context eid)]
      (if-let [skill (npc-choose-skill @eid effect-ctx)]
        [[:tx/event eid :start-action [skill effect-ctx]]]
        [[:tx/event eid :movement-direction (or (potential-field/find-direction ctx/grid eid)
                                                [0 0])]]))))
