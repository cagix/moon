(ns moon.entity.npc.idle
  (:require [moon.effect :as effect]
            [moon.entity :as entity]
            [moon.entity.faction :as faction]
            [moon.entity.follow-ai :as follow-ai]
            [moon.world :as world]
            [moon.world.grid :as grid]))

(defn- nearest-enemy [entity]
  (grid/nearest-entity @(grid/cell (entity/tile entity))
                       (faction/enemy entity)))

(defn- effect-ctx [eid]
  (let [entity @eid
        target (nearest-enemy entity)
        target (when (and target (world/line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target (entity/direction entity @target))}))

(comment
 (let [eid (entity/get-entity 76)
       effect-ctx (effect-ctx eid)]
   (npc-choose-skill effect-ctx @eid))
 )

(defn- npc-choose-skill [entity]
  {:pre [(bound? #'effect/source)]}
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (effect/skill-usable-state entity %))
                     (effect/useful? (:skill/effects %))))
       first))

(defc :npc-idle
  {:let {:keys [eid]}}
  (entity/->v [[_ eid]]
    {:eid eid})

  (entity/tick [_ eid]
    (let [effect-ctx (effect-ctx eid)]
      (if-let [skill (effect/with-ctx effect-ctx
                       (assert (bound? #'effect/source))
                       (npc-choose-skill @eid))]
        [[:tx/event eid :start-action [skill effect-ctx]]]
        [[:tx/event eid :movement-direction (follow-ai/direction-vector eid)]]))))
