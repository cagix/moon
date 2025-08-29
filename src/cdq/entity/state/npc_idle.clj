(ns cdq.entity.state.npc-idle
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.world :as w]))

(defn- npc-choose-skill [world entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (entity/skill-usable-state entity % effect-ctx))
                     (effect/applicable-and-useful? world effect-ctx (:skill/effects %))))
       first))

(defn tick! [_ eid world]
  (let [effect-ctx (w/npc-effect-ctx world eid)]
    (if-let [skill (npc-choose-skill world @eid effect-ctx)]
      [[:tx/event eid :start-action [skill effect-ctx]]]
      [[:tx/event eid :movement-direction (or (w/potential-field-find-direction world eid)
                                              [0 0])]])))
