(ns cdq.entity.state.npc-idle
  (:require [cdq.cell :as cell]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.potential-field.movement :as potential-field]
            [cdq.utils :refer [defcomponent]]
            [cdq.vector2 :as v]))

(defn- npc-choose-skill [ctx entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (entity/skill-usable-state entity % effect-ctx))
                     (effect/applicable-and-useful? ctx effect-ctx (:skill/effects %))))
       first))

(defn- npc-effect-context [{:keys [ctx/grid]} eid]
  (let [entity @eid
        target (cell/nearest-entity @(grid (mapv int (:position entity)))
                                    (entity/enemy entity))
        target (when (and target
                          (entity/line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (v/direction (:position entity)
                                             (:position @target)))}))

(defcomponent :npc-idle
  (entity/tick! [_ eid {:keys [ctx/grid]
                        :as ctx}]
    (let [effect-ctx (npc-effect-context ctx eid)]
      (if-let [skill (npc-choose-skill ctx @eid effect-ctx)]
        [[:tx/event eid :start-action [skill effect-ctx]]]
        [[:tx/event eid :movement-direction (or (potential-field/find-direction grid eid)
                                                [0 0])]]))))
