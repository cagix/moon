(ns cdq.world.tick-entities
  (:require cdq.entity.movement.tick
            cdq.entity.projectile-collision.tick
            [cdq.creature :as creature]
            [cdq.effect :as effect]
            [cdq.entity.animation :as animation]
            [cdq.entity.body :as body]
            [cdq.entity.stats :as stats]
            [cdq.timer :as timer]
            [cdq.world.grid :as grid]
            [cdq.world.grid.cell :as cell]
            [cdq.world.raycaster :as raycaster]
            [cdq.world.potential-fields-movement :as potential-fields-movement]))

(defn- tick-entities!*
  [{:keys [world/active-entities]
    :as world}
   k->tick-fn]
  (mapcat (fn [eid]
            (mapcat (fn [[k v]]
                      (when-let [f (k->tick-fn k)]
                        (f v eid world)))
                    @eid))
          active-entities))

(comment
 (= (tick-entities!* {:world/active-entities [(atom {:firstk :foo
                                                     :secondk :bar})
                                              (atom {:firstk2 :foo2
                                                     :secondk2 :bar2})]}
                     {:firstk (fn [v eid world]
                                [[:foo/bar]])
                      :secondk (fn [v eid world]
                                 [[:foo/barz]
                                  [:asdf]])
                      :firstk2 (fn [v eid world]
                                 nil)
                      :secondk2 (fn [v eid world]
                                  [[:asdf2] [:asdf3]])})
    [[:foo/bar]
     [:foo/barz]
     [:asdf]
     [:asdf2]
     [:asdf3]])
 )

(defn- npc-choose-skill [world entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (creature/skill-usable-state entity % effect-ctx))
                     (->> (:skill/effects %)
                          (filter (fn [e] (effect/applicable? e effect-ctx)))
                          (some (fn [e] (effect/useful? e effect-ctx world))))))
       first))

(defn- npc-effect-ctx
  [{:keys [world/grid]
    :as world}
   eid]
  (let [entity @eid
        target (grid/nearest-enemy grid entity)
        target (when (and target
                          (raycaster/line-of-sight? world entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (body/direction (:entity/body entity)
                                                (:entity/body @target)))}))

(defn- update-effect-ctx
  [world {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (raycaster/line-of-sight? world @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(def ^:private k->tick-fn
  {:entity/alert-friendlies-after-duration (fn
                                             [{:keys [counter faction]}
                                              eid
                                              {:keys [world/elapsed-time
                                                      world/grid]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               (cons [:tx/mark-destroyed eid]
                                                     (for [friendly-eid (->> {:position (:body/position (:entity/body @eid))
                                                                              :radius 4}
                                                                             (grid/circle->entities grid)
                                                                             (filter #(= (:entity/faction @%) faction)))]
                                                       [:tx/event friendly-eid :alert]))))

   :entity/animation                       (fn [animation eid {:keys [world/delta-time]}]
                                             [[:tx/assoc eid :entity/animation (animation/tick animation delta-time)]
                                              (when (and (:delete-after-stopped? animation)
                                                         (animation/stopped? animation))
                                                [:tx/mark-destroyed eid])])

   :entity/delete-after-duration           (fn [counter eid {:keys [world/elapsed-time]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               [[:tx/mark-destroyed eid]]))

   :entity/movement                        cdq.entity.movement.tick/txs
   :entity/projectile-collision            cdq.entity.projectile-collision.tick/txs

   :entity/skills                          (fn [skills eid {:keys [world/elapsed-time]}]
                                             (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
                                                   :when (and cooling-down?
                                                              (timer/stopped? elapsed-time cooling-down?))]
                                               [:tx/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] false]))

   :active-skill                           (fn
                                             [{:keys [skill effect-ctx counter]}
                                              eid
                                              {:keys [world/elapsed-time]
                                               :as world}]
                                             (let [effect-ctx (update-effect-ctx world effect-ctx)]
                                               (cond
                                                (not (seq (filter #(effect/applicable? % effect-ctx)
                                                                  (:skill/effects skill))))
                                                [[:tx/event eid :action-done]]

                                                (timer/stopped? elapsed-time counter)
                                                [[:tx/effect effect-ctx (:skill/effects skill)]
                                                 [:tx/event eid :action-done]])))

   :npc-idle                               (fn [_ eid world]
                                             (let [effect-ctx (npc-effect-ctx world eid)]
                                               (if-let [skill (npc-choose-skill world @eid effect-ctx)]
                                                 [[:tx/event eid :start-action [skill effect-ctx]]]
                                                 [[:tx/event eid :movement-direction (or (potential-fields-movement/find-direction world eid)
                                                                                         [0 0])]])))

   :npc-moving                             (fn [{:keys [timer]} eid {:keys [world/elapsed-time]}]
                                             (when (timer/stopped? elapsed-time timer)
                                               [[:tx/event eid :timer-finished]]))

   :npc-sleeping                           (fn [_ eid {:keys [world/grid]}]
                                             (let [entity @eid]
                                               (when-let [distance (grid/nearest-enemy-distance grid entity)]
                                                 (when (<= distance (stats/get-stat-value (:entity/stats entity) :stats/aggro-range))
                                                   [[:tx/event eid :alert]]))))

   :stunned                                (fn [{:keys [counter]} eid {:keys [world/elapsed-time]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               [[:tx/event eid :effect-wears-off]]))

   :entity/string-effect                   (fn
                                             [{:keys [counter]}
                                              eid
                                              {:keys [world/elapsed-time]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               [[:tx/dissoc eid :entity/string-effect]]))

   :entity/temp-modifier                   (fn
                                             [{:keys [modifiers counter]}
                                              eid
                                              {:keys [world/elapsed-time]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               [[:tx/dissoc eid :entity/temp-modifier]
                                                [:tx/update eid :entity/stats stats/remove-mods modifiers]]))})

(defn do! [world]
  (tick-entities!* world k->tick-fn))
