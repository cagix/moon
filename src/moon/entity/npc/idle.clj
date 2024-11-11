(ns moon.entity.npc.idle
  (:require [moon.effects :as effects]
            [moon.body :as body]
            [moon.entity.faction :as faction]
            [moon.entity.fsm :as fsm]
            [moon.entity.skills :as skills]
            [moon.follow-ai :as follow-ai]
            [moon.world.grid :as grid]
            [moon.world.line-of-sight :refer [line-of-sight?]]))

(defn- nearest-enemy [entity]
  (grid/nearest-entity @(grid/cell (body/tile entity))
                       (faction/enemy entity)))

(defn- effect-ctx [eid]
  (let [entity @eid
        target (nearest-enemy entity)
        target (when (and target (line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target (body/direction entity @target))}))

(comment
 (let [eid (entity/get-entity 76)
       effect-ctx (effect-ctx eid)]
   (npc-choose-skill effect-ctx @eid))
 )

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skills/usable-state entity % ctx))
                     (effects/useful? ctx (:skill/effects %))))
       first))

(defn ->v [eid]
  {:eid eid})

(defn tick [_ eid]
  (let [effect-ctx (effect-ctx eid)]
    (if-let [skill (npc-choose-skill @eid effect-ctx)]
      (fsm/event eid :start-action [skill effect-ctx])
      (fsm/event eid :movement-direction (or (follow-ai/direction-vector eid) [0 0])))))
