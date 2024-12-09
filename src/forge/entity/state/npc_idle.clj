(ns forge.entity.state.npc-idle
  (:require [anvil.body :as body]
            [anvil.effect :as effect]
            [anvil.fsm :as fsm]
            [anvil.faction :as faction]
            [anvil.skill :as skill]
            [anvil.potential-field :as potential-field]
            [anvil.world :as world :refer [nearest-entity line-of-sight?]]))

(defn- nearest-enemy [entity]
  (nearest-entity @(get world/grid (body/tile entity))
                  (faction/enemy entity)))

(defn- npc-effect-ctx [eid]
  (let [entity @eid
        target (nearest-enemy entity)
        target (when (and target
                          (line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (body/direction entity @target))}))

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
      (fsm/event eid :start-action [skill effect-ctx])
      (fsm/event eid :movement-direction (or (potential-field/find-direction eid) [0 0])))))
