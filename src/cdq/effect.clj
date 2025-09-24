(ns cdq.effect
  (:require cdq.effects.projectile
            cdq.effects.target-all
            [cdq.effects.target-entity :as target-entity]
            [cdq.entity :as entity]
            [cdq.entity.stats]
            [cdq.entity.faction :as faction]
            [cdq.stats :as stats]
            [cdq.timer :as timer]
            [clojure.rand :refer [rand-int-between]])
  (:import (clojure.lang APersistentVector)))

(defprotocol Effect
  (applicable? [_ effect-ctx])
  (useful?     [_ effect-ctx ctx])
  (handle      [_ effect-ctx ctx]))

(defn filter-applicable? [effect-ctx effect]
  (filter #(applicable? % effect-ctx) effect))

(defn some-applicable? [effect-ctx effect]
  (seq (filter-applicable? effect-ctx effect)))

(defn applicable-and-useful? [ctx effect-ctx effect]
  (->> effect
       (filter-applicable? effect-ctx)
       (some #(useful? % effect-ctx ctx))))

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

(def ^:private k->fn
  {:effects/audiovisual {:applicable? (fn [_ {:keys [effect/target-position]}]
                                        target-position)
                         :useful? (fn [_ _effect-ctx _ctx]
                                    false)
                         :handle (fn [[_ audiovisual] {:keys [effect/target-position]} _ctx]
                                   [[:tx/audiovisual target-position audiovisual]])}
   :effects/projectile {:applicable? cdq.effects.projectile/applicable?
                        :useful? cdq.effects.projectile/useful?
                        :handle cdq.effects.projectile/handle}
   :effects/sound {:applicable? (fn [_ _ctx]
                                  true)
                   :useful? (fn [_ _effect-ctx _ctx]
                              false)
                   :handle (fn [[_ sound] _effect-ctx _ctx]
                             [[:tx/sound sound]])}
   :effects/spawn {:applicable? (fn [_ {:keys [effect/source effect/target-position]}]
                                  (and (:entity/faction @source)
                                       target-position))
                   :handle (fn [[_ {:keys [property/id] :as property}]
                                {:keys [effect/source effect/target-position]}
                                _ctx]
                             [[:tx/spawn-creature {:position target-position
                                                   :creature-property property
                                                   :components {:entity/fsm {:fsm :fsms/npc
                                                                             :initial-state :npc-idle}
                                                                :entity/faction (:entity/faction @source)}}]])}
   :effects/target-all {:applicable? cdq.effects.target-all/applicable?
                        :useful? cdq.effects.target-all/useful?
                        :handle cdq.effects.target-all/handle}
   :effects/target-entity {:applicable? (fn [[_ {:keys [entity-effects]}] {:keys [effect/target] :as effect-ctx}]
                                          (and target
                                               (seq (filter-applicable? effect-ctx entity-effects))))
                           :useful? (fn [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} _ctx]
                                      (target-entity/in-range? @source @target maxrange))
                           :handle (fn [[_ {:keys [maxrange entity-effects]}]
                                        {:keys [effect/source effect/target] :as effect-ctx}
                                        _ctx]
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
                                :useful? (fn [_ _effect-ctx _ctx]
                                           false)
                                :handle (fn [[_ audiovisual] {:keys [effect/target]} _ctx]
                                          [[:tx/audiovisual (entity/position @target) audiovisual]])}
   :effects.target/convert {:applicable? (fn [_ {:keys [effect/source effect/target]}]
                                           (and target
                                                (= (:entity/faction @target)
                                                   (faction/enemy (:entity/faction @source)))))
                            :handle (fn [_ {:keys [effect/source effect/target]} _ctx]
                                      [[:tx/assoc target :entity/faction (:entity/faction @source)]])}


   :effects.target/damage {:applicable? (fn [_ {:keys [effect/target]}]
                                          (and target
                                               #_(:entity/hp @target))) ; not exist anymore ... bugfix .... -> is 'creature?'

                           :handle (fn [[_ damage]
                                        {:keys [effect/source effect/target]}
                                        _ctx]
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
                         :handle (fn [_ {:keys [effect/target]} _ctx]
                                   [[:tx/event target :kill]])}
   :effects.target/melee-damage {:applicable? (fn [_ {:keys [effect/source] :as effect-ctx}]
                                                (applicable? (melee-damage-effect @source) effect-ctx))
                                 :handle (fn [_ {:keys [effect/source] :as effect-ctx} ctx]
                                           (handle (melee-damage-effect @source) effect-ctx ctx))}
   :effects.target/spiderweb (let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
                                   duration 5]

                               {:applicable? (fn [_ {:keys [effect/target]}]
                                               ; TODO has stats , for mod-add
                                               ; e,g, spiderweb on projectile leads to error
                                               (:creature/stats @target))

                                ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
                                :handle (fn [_
                                             {:keys [effect/target]}
                                             {:keys [ctx/world]}]
                                          (let [{:keys [world/elapsed-time]} world]
                                            (when-not (:entity/temp-modifier @target)
                                              [[:tx/assoc target :entity/temp-modifier {:modifiers modifiers
                                                                                        :counter (timer/create elapsed-time duration)}]
                                               [:tx/mod-add target modifiers]])))})
   :effects.target/stun {:applicable? (fn [_ {:keys [effect/target]}]
                                        (and target
                                             (:entity/fsm @target)))
                         :handle      (fn [[_ duration] {:keys [effect/target]} _ctx]
                                        [[:tx/event target :stun duration]])}})

(extend APersistentVector
  Effect
  {:applicable? (fn [{k 0 :as component} effect-ctx]
                  ((:applicable? (k->fn k)) component effect-ctx))

   :handle (fn [{k 0 :as component} effect-ctx ctx]
             ((:handle (k->fn k)) component effect-ctx ctx))

   :useful? (fn [{k 0 :as component} effect-ctx ctx]
              (if-let [f (:useful? (k->fn k))]
                (f component effect-ctx ctx)
                true))})
