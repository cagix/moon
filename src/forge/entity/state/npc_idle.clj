(ns forge.entity.state.npc-idle
  (:require [clojure.utils :refer [defmethods]]
            [forge.effect :refer [effects-applicable? effects-useful?]]
            [forge.entity.body :refer [e-tile e-direction]]
            [forge.entity.faction :as faction]
            [forge.entity.fsm :refer [send-event]]
            [forge.entity.mana :refer [mana-value]]
            [forge.world :refer [line-of-sight?]]
            [forge.world.grid :refer [nearest-entity world-grid]]
            [forge.world.potential-fields :as potential-fields]))

(defn- nearest-enemy [entity]
  (nearest-entity @(get world-grid (e-tile entity))
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
                                (e-direction entity @target))}))

(comment
 (let [eid (ids->eids 76)
       effect-ctx (npc-effect-ctx eid)]
   (npc-choose-skill effect-ctx @eid))
 )

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana-value entity))))

(defn- skill-usable-state
  [entity
   {:keys [skill/cooling-down? skill/effects] :as skill}
   effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effects-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill-usable-state entity % ctx))
                     (effects-useful? ctx (:skill/effects %))))
       first))

(defmethods :npc-idle
  (->v [[_ eid]]
    {:eid eid})

  (tick [_ eid]
    (let [effect-ctx (npc-effect-ctx eid)]
      (if-let [skill (npc-choose-skill @eid effect-ctx)]
        (send-event eid :start-action [skill effect-ctx])
        (send-event eid :movement-direction (or (potential-fields/find-direction eid) [0 0]))))))
