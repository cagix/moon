(ns cdq.effects
  (:require [cdq.walk :as walk]))

(def method-map*
  '{:effects/audiovisual {:applicable? cdq.effects.audiovisual/applicable?
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
                         :handle cdq.effects.target-all/handle
                         :render cdq.effects.target-all/render}
    :effects/target-entity {:applicable? cdq.effects.target-entity/applicable?
                            :useful? cdq.effects.target-entity/useful?
                            :handle cdq.effects.target-entity/handle
                            :render cdq.effects.target-entity/render}
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



(def method-map
  (walk/require-resolve-symbols method-map*))
