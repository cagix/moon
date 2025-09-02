(ns cdq.entity.state.npc-idle
  (:require [cdq.gdx.math.vector2 :as v]
            [cdq.world.grid :as grid]
            [cdq.raycaster :as raycaster]
            [cdq.world.effect :as effect]
            [cdq.world.entity :as entity]
            [cdq.world.entity.skill :as skill]
            [cdq.world.potential-fields.movement :as potential-fields.movement]))

(defn- npc-effect-ctx
  [{:keys [world/raycaster
           world/grid]}
   eid]
  (let [entity @eid
        target (grid/nearest-enemy grid entity)
        target (when (and target
                          (raycaster/line-of-sight? raycaster entity @target))
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
       (filter #(and (= :usable (skill/usable-state entity % effect-ctx))
                     (effect/applicable-and-useful? world effect-ctx (:skill/effects %))))
       first))

(defn tick! [_ eid world]
  (let [effect-ctx (npc-effect-ctx world eid)
        grid (:world/grid world)]
    (if-let [skill (npc-choose-skill world @eid effect-ctx)]
      [[:tx/event eid :start-action [skill effect-ctx]]]
      [[:tx/event eid :movement-direction (or (potential-fields.movement/find-direction grid eid)
                                              [0 0])]])))
