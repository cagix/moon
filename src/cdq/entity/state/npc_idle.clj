(ns cdq.entity.state.npc-idle
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.ctx :as ctx]
            [cdq.w :as w]))

(defn- npc-choose-skill [ctx entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (entity/skill-usable-state entity % effect-ctx))
                     (effect/applicable-and-useful? ctx effect-ctx (:skill/effects %))))
       first))

(defn tick! [_ eid {:keys [ctx/world] :as ctx}]
  (let [effect-ctx (ctx/npc-effect-ctx ctx eid)]
    (if-let [skill (npc-choose-skill ctx @eid effect-ctx)]
      [[:tx/event eid :start-action [skill effect-ctx]]]
      [[:tx/event eid :movement-direction (or (w/potential-field-find-direction world eid)
                                              [0 0])]])))
