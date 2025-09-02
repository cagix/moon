(ns cdq.entity.state.npc-idle
  (:require [cdq.ctx.world :as w]
            [cdq.gdx.math.vector2 :as v]
            [cdq.world.effect :as effect]
            [cdq.world.entity :as entity]))

(defn- npc-effect-ctx [world eid]
  (let [entity @eid
        target (w/nearest-enemy world entity)
        target (when (and target
                          (w/line-of-sight? world entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (v/direction (entity/position entity)
                                             (entity/position @target)))}))

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
  (let [effect-ctx (npc-effect-ctx world eid)]
    (if-let [skill (npc-choose-skill world @eid effect-ctx)]
      [[:tx/event eid :start-action [skill effect-ctx]]]
      [[:tx/event eid :movement-direction (or (w/potential-field-find-direction world eid)
                                              [0 0])]])))
