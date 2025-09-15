(ns cdq.entity.state.npc-idle
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.gdx.math.vector2 :as v]
            [cdq.world.grid :as grid]
            [cdq.raycaster :as raycaster]
            [cdq.skill :as skill]
            [cdq.potential-fields.movement :as potential-fields.movement]))

(defn- npc-choose-skill [ctx entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % effect-ctx))
                     (effect/applicable-and-useful? ctx effect-ctx (:skill/effects %))))
       first))

(defn- npc-effect-ctx
  [{:keys [ctx/raycaster
           ctx/world]}
   eid]
  (let [entity @eid
        target (grid/nearest-enemy (:world/grid world) entity)
        target (when (and target
                          (raycaster/line-of-sight? raycaster entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (v/direction (entity/position entity)
                                             (entity/position @target)))}))

(defn tick! [_ eid {:keys [ctx/world] :as ctx}]
  (let [effect-ctx (npc-effect-ctx ctx eid)]
    (if-let [skill (npc-choose-skill ctx @eid effect-ctx)]
      [[:tx/event eid :start-action [skill effect-ctx]]]
      [[:tx/event eid :movement-direction (or (potential-fields.movement/find-direction (:world/grid world) eid)
                                              [0 0])]])))
