(ns forge.entity.state.npc-idle
  (:require [anvil.effect :as effect]
            [anvil.entity :as entity :refer [send-event]]
            [anvil.world :as world :refer [nearest-entity line-of-sight?]]
            [forge.skill :as skill]))

(defn- nearest-enemy [entity]
  (nearest-entity @(get world/grid (entity/tile entity))
                  (entity/enemy entity)))

(defn- npc-effect-ctx [eid]
  (let [entity @eid
        target (nearest-enemy entity)
        target (when (and target
                          (line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(comment
 (let [eid (ids->eids 76)
       effect-ctx (npc-effect-ctx eid)]
   (npc-choose-skill effect-ctx @eid))
 )

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (effect/useful? ctx (:skill/effects %))))
       first))

(defn ->v [[_ eid]]
  {:eid eid})

(defn tick [_ eid]
  (let [effect-ctx (npc-effect-ctx eid)]
    (if-let [skill (npc-choose-skill @eid effect-ctx)]
      (send-event eid :start-action [skill effect-ctx])
      (send-event eid :movement-direction (or (world/find-direction eid) [0 0])))))
