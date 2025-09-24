(ns cdq.create.world.effects
  (:require [cdq.effect :as effect]
            [cdq.effects.target-all :as target-all]
            [cdq.effects.target-entity :as target-entity]
            [cdq.entity :as entity]
            [cdq.entity.stats]
            [cdq.entity.faction :as faction]
            [cdq.stats :as stats]
            [cdq.timer :as timer]
            [cdq.world :as world]
            [clojure.math.vector2 :as v]
            [clojure.rand :refer [rand-int-between]])
  (:import (clojure.lang APersistentVector)))

; not in stats because projectile as source doesnt have stats
; FIXME I don't see it triggering with 10 armor save ... !
(defn- effective-armor-save [source-stats target-stats]
  (max (- (or (stats/get-stat-value source-stats :entity/armor-save)   0)
          (or (stats/get-stat-value target-stats :entity/armor-pierce) 0))
       0))

(comment

 (effective-armor-save {} {:entity/modifiers {:modifiers/armor-save {:op/inc 10}}
                           :entity/armor-save 0})
 ; broken
 (let [source* {:entity/armor-pierce 0.4}
       target* {:entity/armor-save   0.5}]
   (effective-armor-save source* target*))
 )

(defn- calc-damage
  ([source target damage]
   (update (calc-damage source damage)
           :damage/min-max
           cdq.entity.stats/apply-max
           (:entity/modifiers target)
           :modifier/damage-receive-max))
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (cdq.entity.stats/apply-min (:entity/modifiers source) :modifier/damage-deal-min)
                (cdq.entity.stats/apply-max (:entity/modifiers source) :modifier/damage-deal-max)))))

(defn- entity->melee-damage [{:keys [creature/stats]}]
  (let [strength (or (stats/get-stat-value stats :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- melee-damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defn- proj-start-point [entity direction size]
  (v/add (entity/position entity)
         (v/scale direction
                  (+ (/ (:body/width (:entity/body entity)) 2) size 0.1))))

(comment
 ; mass shooting
 (for [direction (map math.vector/normalise
                      [[1 0]
                       [1 1]
                       [1 -1]
                       [0 1]
                       [0 -1]
                       [-1 -1]
                       [-1 1]
                       [-1 0]])]
   [:world/projectile projectile-id ...]
   )
 )

(def ^:private k->fn
  {:effects/audiovisual {:applicable? (fn [_ {:keys [effect/target-position]}]
                                        target-position)
                         :useful? (fn [_ _effect-ctx _world]
                                    false)
                         :handle (fn [[_ audiovisual] {:keys [effect/target-position]} _world]
                                   [[:tx/audiovisual target-position audiovisual]])}

   :effects/projectile {:applicable?
                        ; TODO for npcs need target -- anyway only with direction
                        (fn [_ {:keys [effect/target-direction]}]
                          target-direction) ; faction @ source also ?

                        :useful?
                        ; TODO valid params direction has to be  non-nil (entities not los player ) ?
                        (fn [[_ {:keys [projectile/max-range] :as projectile}]
                             {:keys [effect/source effect/target]}
                             world]
                          (let [source-p (entity/position @source)
                                target-p (entity/position @target)]
                            ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
                            (and (not (world/path-blocked? world source-p target-p (:projectile/size projectile)))
                                 ; TODO not taking into account body sizes
                                 (< (v/distance source-p ; entity/distance function protocol EntityPosition
                                                target-p)
                                    max-range))))

                        :handle (fn [[_ projectile] {:keys [effect/source effect/target-direction]} _world]
                                  [[:tx/spawn-projectile
                                    {:position (proj-start-point @source
                                                                 target-direction
                                                                 (:projectile/size projectile))
                                     :direction target-direction
                                     :faction (:entity/faction @source)}
                                    projectile]])}

   :effects/sound {:applicable? (fn [_ _world]
                                  true)
                   :useful? (fn [_ _effect-ctx _world]
                              false)
                   :handle (fn [[_ sound] _effect-ctx _world]
                             [[:tx/sound sound]])}

   :effects/spawn {:applicable? (fn [_ {:keys [effect/source effect/target-position]}]
                                  (and (:entity/faction @source)
                                       target-position))
                   :handle (fn [[_ {:keys [property/id] :as property}]
                                {:keys [effect/source effect/target-position]}
                                _world]
                             [[:tx/spawn-creature {:position target-position
                                                   :creature-property property
                                                   :components {:entity/fsm {:fsm :fsms/npc
                                                                             :initial-state :npc-idle}
                                                                :entity/faction (:entity/faction @source)}}]])}

   :effects/target-all {:applicable? (fn [_ _] ; TODO check ..
                                       true)
                        :useful? (fn [_ _effect-ctx _world]
                                   false)
                        :handle (fn [[_ {:keys [entity-effects]}]
                                     {:keys [effect/source]}
                                     world]
                                  (let [{:keys [world/active-entities]} world
                                        source* @source]
                                    (apply concat
                                           (for [target (target-all/affected-targets active-entities world source*)]
                                             [[:tx/spawn-line
                                               {:start (:body/position (:entity/body source*)) #_(start-point source* target*)
                                                :end (:body/position (:entity/body @target))
                                                :duration 0.05
                                                :color [1 0 0 0.75]
                                                :thick? true}]
                                              [:tx/effect
                                               {:effect/source source
                                                :effect/target target}
                                               entity-effects]]))))}

   :effects/target-entity {:applicable? (fn [[_ {:keys [entity-effects]}] {:keys [effect/target] :as effect-ctx}]
                                          (and target
                                               (seq (effect/filter-applicable? effect-ctx entity-effects))))
                           :useful? (fn [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} _world]
                                      (target-entity/in-range? @source @target maxrange))
                           :handle (fn [[_ {:keys [maxrange entity-effects]}]
                                        {:keys [effect/source effect/target] :as effect-ctx}
                                        _world]
                                     (let [source* @source
                                           target* @target]
                                       (if (target-entity/in-range? source* target* maxrange)
                                         [[:tx/spawn-line {:start (target-entity/start-point source* target*)
                                                           :end (entity/position target*)
                                                           :duration 0.05
                                                           :color [1 0 0 0.75]
                                                           :thick? true}]
                                          [:tx/effect effect-ctx entity-effects]]
                                         [[:tx/audiovisual
                                           (target-entity/end-point source* target* maxrange)
                                           :audiovisuals/hit-ground]])))}

   :effects.target/audiovisual {:applicable? (fn [_ {:keys [effect/target]}]
                                               target)
                                :useful? (fn [_ _effect-ctx _world]
                                           false)
                                :handle (fn [[_ audiovisual] {:keys [effect/target]} _world]
                                          [[:tx/audiovisual (entity/position @target) audiovisual]])}

   :effects.target/convert {:applicable? (fn [_ {:keys [effect/source effect/target]}]
                                           (and target
                                                (= (:entity/faction @target)
                                                   (faction/enemy (:entity/faction @source)))))
                            :handle (fn [_ {:keys [effect/source effect/target]} _world]
                                      [[:tx/assoc target :entity/faction (:entity/faction @source)]])}


   :effects.target/damage {:applicable? (fn [_ {:keys [effect/target]}]
                                          (and target
                                               #_(:entity/hp @target))) ; not exist anymore ... bugfix .... -> is 'creature?'

                           :handle (fn [[_ damage]
                                        {:keys [effect/source effect/target]}
                                        _world]
                                     (let [source* @source
                                           target* @target
                                           hp (stats/get-hitpoints (:creature/stats target*))]
                                       (cond
                                        (zero? (hp 0))
                                        nil

                                        ; TODO find a better way
                                        (not (:creature/stats target*))
                                        nil

                                        (and (:creature/stats source*)
                                             (:creature/stats target*)
                                             (< (rand) (effective-armor-save (:creature/stats source*)
                                                                             (:creature/stats target*))))
                                        [[:tx/add-text-effect target "[WHITE]ARMOR" 0.3]]

                                        :else
                                        (let [min-max (:damage/min-max (calc-damage (:creature/stats source*)
                                                                                    (:creature/stats target*)
                                                                                    damage))
                                              dmg-amount (rand-int-between min-max)
                                              new-hp-val (max (- (hp 0) dmg-amount)
                                                              0)]
                                          [[:tx/assoc-in target [:creature/stats :entity/hp 0] new-hp-val]
                                           [:tx/event    target (if (zero? new-hp-val) :kill :alert)]
                                           [:tx/audiovisual (entity/position target*) :audiovisuals/damage]
                                           [:tx/add-text-effect target (str "[RED]" dmg-amount "[]") 0.3]]))))}

   :effects.target/kill {:applicable? (fn [_ {:keys [effect/target]}]
                                        (and target
                                             (:entity/fsm @target)))
                         :handle (fn [_ {:keys [effect/target]} _world]
                                   [[:tx/event target :kill]])}

   :effects.target/melee-damage {:applicable? (fn [_ {:keys [effect/source] :as effect-ctx}]
                                                (effect/applicable? (melee-damage-effect @source) effect-ctx))
                                 :handle (fn [_ {:keys [effect/source] :as effect-ctx} world]
                                           (effect/handle (melee-damage-effect @source) effect-ctx world))}

   :effects.target/spiderweb (let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
                                   duration 5]

                               {:applicable? (fn [_ {:keys [effect/target]}]
                                               ; TODO has stats , for mod-add
                                               ; e,g, spiderweb on projectile leads to error
                                               (:creature/stats @target))

                                ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
                                :handle (fn [_
                                             {:keys [effect/target]}
                                             world]
                                          (let [{:keys [world/elapsed-time]} world]
                                            (when-not (:entity/temp-modifier @target)
                                              [[:tx/assoc target :entity/temp-modifier {:modifiers modifiers
                                                                                        :counter (timer/create elapsed-time duration)}]
                                               [:tx/mod-add target modifiers]])))})

   :effects.target/stun {:applicable? (fn [_ {:keys [effect/target]}]
                                        (and target
                                             (:entity/fsm @target)))
                         :handle      (fn [[_ duration] {:keys [effect/target]} _world]
                                        [[:tx/event target :stun duration]])}})

(extend APersistentVector
  effect/Effect
  {:applicable? (fn [{k 0 :as component} effect-ctx]
                  ((:applicable? (k->fn k)) component effect-ctx))

   :handle (fn [{k 0 :as component} effect-ctx world]
             ((:handle (k->fn k)) component effect-ctx world))

   :useful? (fn [{k 0 :as component} effect-ctx world]
              (if-let [f (:useful? (k->fn k))]
                (f component effect-ctx world)
                true))})
