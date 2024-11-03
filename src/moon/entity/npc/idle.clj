(ns moon.entity.npc.idle
  (:require [moon.effect :as effect]
            [moon.body :as body]
            [moon.faction :as faction]
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

(defn ->v [[_ eid]]
  {:eid eid})

(defn tick [_ eid]
  (let [effect-ctx (effect-ctx eid)]
    (if-let [skill (effect/with-ctx effect-ctx
                     (assert (bound? #'effect/source))
                     (npc-choose-skill @eid))]
      [[:entity/fsm eid :start-action [skill effect-ctx]]]
      [[:entity/fsm eid :movement-direction (follow-ai/direction-vector eid)]])))
