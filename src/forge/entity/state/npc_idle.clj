(ns forge.entity.state.npc-idle
  (:require [anvil.body :as body]
            [anvil.effect :as effect]
            [anvil.entity :refer [line-of-sight? projectile-size]]
            [anvil.fsm :as fsm]
            [anvil.faction :as faction]
            [anvil.grid :as grid]
            [anvil.raycaster :refer [path-blocked?]]
            [anvil.skill :as skill]
            [anvil.potential-field :as potential-field]
            [clojure.component :refer [defsystem]]
            [clojure.gdx.math.vector2 :as v]))

(defn- nearest-enemy [entity]
  (grid/nearest-entity @(grid/get (body/tile entity))
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

(defsystem useful?)
(defmethod useful? :default [_ _ctx] true)

(defmethod useful? :effects.target/audiovisual [_ _]
  false)

; TODO valid params direction has to be  non-nil (entities not los player ) ?
(defmethod useful? :effects/projectile
  [[_ {:keys [projectile/max-range] :as projectile}]
   {:keys [effect/source effect/target]}]
  (let [source-p (:position @source)
        target-p (:position @target)]
    ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
    (and (not (path-blocked? ; TODO test
                             source-p
                             target-p
                             (projectile-size projectile)))
         ; TODO not taking into account body sizes
         (< (v/distance source-p ; entity/distance function protocol EntityPosition
                        target-p)
            max-range))))

(defmethod useful? :effects/target-all [_ _]
  ; TODO
  false)

(defmethod useful? :effects/target-entity
  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]}]
  (effect/in-range? @source @target maxrange))

(defn- some-useful-and-applicable? [ctx effects]
  (->> effects
       (effect/filter-applicable? ctx)
       (some #(useful? % ctx))))

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (some-useful-and-applicable? ctx (:skill/effects %))))
       first))

(defn ->v [[_ eid]]
  {:eid eid})

(defn tick [_ eid]
  (let [effect-ctx (npc-effect-ctx eid)]
    (if-let [skill (npc-choose-skill @eid effect-ctx)]
      (fsm/event eid :start-action [skill effect-ctx])
      (fsm/event eid :movement-direction (or (potential-field/find-direction eid) [0 0])))))
