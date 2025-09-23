(ns cdq.create.load-effects
  (:require [cdq.effect :as effect]
            cdq.effects.audiovisual
            cdq.effects.projectile
            cdq.effects.sound
            cdq.effects.spawn
            cdq.effects.target-all
            cdq.effects.target-entity
            cdq.effects.target.audiovisual
            cdq.effects.target.convert
            cdq.effects.target.damage
            cdq.effects.target.kill
            cdq.effects.target.melee-damage
            cdq.effects.target.spiderweb
            cdq.effects.target.stun)
  (:import (clojure.lang APersistentVector)))

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
   :effects/target-entity {:applicable? cdq.effects.target-entity/applicable?
                           :useful? cdq.effects.target-entity/useful?
                           :handle cdq.effects.target-entity/handle}
   :effects.target/audiovisual {:applicable? cdq.effects.target.audiovisual/applicable?
                                :useful? cdq.effects.target.audiovisual/useful?
                                :handle cdq.effects.target.audiovisual/handle}
   :effects.target/convert {:applicable? cdq.effects.target.convert/applicable?
                            :handle cdq.effects.target.convert/handle}
   :effects.target/damage {:applicable? cdq.effects.target.damage/applicable?
                           :handle cdq.effects.target.damage/handle}
   :effects.target/kill {:applicable? cdq.effects.target.kill/applicable?
                         :handle cdq.effects.target.kill/handle}
   :effects.target/melee-damage {:applicable? cdq.effects.target.melee-damage/applicable?
                                 :handle cdq.effects.target.melee-damage/handle}
   :effects.target/spiderweb {:applicable? cdq.effects.target.spiderweb/applicable?
                              :handle      cdq.effects.target.spiderweb/handle}
   :effects.target/stun {:applicable? cdq.effects.target.stun/applicable?
                         :handle      cdq.effects.target.stun/handle}})

(defn do! []
  (extend APersistentVector
    effect/Effect
    {:applicable? (fn [{k 0 :as component} effect-ctx]
                    ((:applicable? (k->fn k)) component effect-ctx))

     :handle (fn [{k 0 :as component} effect-ctx ctx]
               ((:handle (k->fn k)) component effect-ctx ctx))

     :useful? (fn [{k 0 :as component} effect-ctx ctx]
                (if-let [f (:useful? (k->fn k))]
                  (f component effect-ctx ctx)
                  true))}))
