(ns cdq.effect)

(defprotocol Effect
  (applicable? [_ effect-ctx])
  (useful?     [_ effect-ctx world])
  (handle      [_ effect-ctx world])
  (draw        [_ effect-ctx ctx]))

(def ^:private k->fn
  '{:effects/audiovisual {:applicable? cdq.effects.audiovisual/applicable?
                          :useful?     cdq.effects.audiovisual/useful?
                          :handle      cdq.effects.audiovisual/handle}

    :effects/projectile {:applicable? cdq.effects.projectile/applicable?
                         :useful?     cdq.effects.projectile/useful?
                         :handle      cdq.effects.projectile/handle}

    :effects/spawn {:applicable? cdq.effects.spawn/applicable?
                    :handle      cdq.effects.spawn/handle}

    :effects/target-all {:applicable? cdq.effects.target-all/applicable?
                         :useful?     cdq.effects.target-all/useful?
                         :handle      cdq.effects.target-all/handle
                         :draw        cdq.effects.target-all/draw}

    :effects/target-entity {:applicable? cdq.effects.target-entity/applicable?
                            :useful?     cdq.effects.target-entity/useful?
                            :handle      cdq.effects.target-entity/handle
                            :draw        cdq.effects.target-entity/draw}

    :effects.target/audiovisual {:applicable? cdq.effects.target.audiovisual/applicable?
                                 :useful?     cdq.effects.target.audiovisual/useful?
                                 :handle      cdq.effects.target.audiovisual/handle}

    :effects.target/convert {:applicable? cdq.effects.target.convert/applicable?
                             :handle      cdq.effects.target.convert/handle}

    :effects.target/damage {:applicable? cdq.effects.target.damage/applicable?
                            :handle      cdq.effects.target.damage/handle}

    :effects.target/kill {:applicable? cdq.effects.target.kill/applicable?
                          :handle      cdq.effects.target.kill/handle}

    :effects.target/melee-damage {:applicable? cdq.effects.target.melee-damage/applicable?
                                  :handle      cdq.effects.target.melee-damage/handle}

    :effects.target/spiderweb {:applicable? cdq.effects.target.spiderweb/applicable?
                               :handle      cdq.effects.target.spiderweb/handle}

    :effects.target/stun {:applicable? cdq.effects.target.stun/applicable?
                          :handle      cdq.effects.target.stun/handle}})

(alter-var-root #'k->fn update-vals
                (fn [k->fn]
                  (update-vals k->fn
                               (fn [sym]
                                 (let [avar (requiring-resolve sym)]
                                   (assert avar sym)
                                   avar)))))

(extend clojure.lang.APersistentVector
  Effect
  {:applicable? (fn [{k 0 :as component} effect-ctx]
                  ((:applicable? (k->fn k)) component effect-ctx))

   :handle (fn [{k 0 :as component} effect-ctx world]
             ((:handle (k->fn k)) component effect-ctx world))

   :useful? (fn [{k 0 :as component} effect-ctx world]
              (if-let [f (:useful? (k->fn k))]
                (f component effect-ctx world)
                true))

   :draw (fn [{k 0 :as component} effect-ctx ctx]
           (if-let [f (:draw (k->fn k))]
             (f component effect-ctx ctx)
             nil))})
