(ns cdq.effect
  (:require cdq.effects.audiovisual
            cdq.effects.projectile
            cdq.effects.sound
            cdq.effects.spawn
            cdq.effects.target-all
            [cdq.effects.target-entity :as target-entity]
            cdq.effects.target.audiovisual
            cdq.effects.target.convert
            cdq.effects.target.damage
            cdq.effects.target.kill
            cdq.effects.target.spiderweb
            cdq.effects.target.stun
            [cdq.entity :as entity]
            [cdq.stats :as stats])
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

(defn- entity->melee-damage [{:keys [creature/stats]}]
  (let [strength (or (stats/get-stat-value stats :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- melee-damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(def ^:private k->fn
  {:effects/audiovisual {:applicable? cdq.effects.audiovisual/applicable?
                         :useful? cdq.effects.audiovisual/useful?
                         :handle cdq.effects.audiovisual/handle}
   :effects/projectile {:applicable? cdq.effects.projectile/applicable?
                        :useful? cdq.effects.projectile/useful?
                        :handle cdq.effects.projectile/handle}
   :effects/sound {:applicable? cdq.effects.sound/applicable?
                   :useful? cdq.effects.sound/useful?
                   :handle cdq.effects.sound/handle}
   :effects/spawn {:applicable? cdq.effects.spawn/applicable?
                   :handle cdq.effects.spawn/handle}
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
   :effects.target/audiovisual {:applicable? cdq.effects.target.audiovisual/applicable?
                                :useful? cdq.effects.target.audiovisual/useful?
                                :handle cdq.effects.target.audiovisual/handle}
   :effects.target/convert {:applicable? cdq.effects.target.convert/applicable?
                            :handle cdq.effects.target.convert/handle}
   :effects.target/damage {:applicable? cdq.effects.target.damage/applicable?
                           :handle cdq.effects.target.damage/handle}
   :effects.target/kill {:applicable? cdq.effects.target.kill/applicable?
                         :handle cdq.effects.target.kill/handle}
   :effects.target/melee-damage {:applicable? (fn [_ {:keys [effect/source] :as effect-ctx}]
                                                (applicable? (melee-damage-effect @source) effect-ctx))
                                 :handle (fn [_ {:keys [effect/source] :as effect-ctx} ctx]
                                           (handle (melee-damage-effect @source) effect-ctx ctx))}
   :effects.target/spiderweb {:applicable? cdq.effects.target.spiderweb/applicable?
                              :handle      cdq.effects.target.spiderweb/handle}
   :effects.target/stun {:applicable? cdq.effects.target.stun/applicable?
                         :handle      cdq.effects.target.stun/handle}})

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
