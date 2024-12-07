(ns forge.effects.spawn
  (:require [clojure.utils :refer [defmethods]]
            [forge.app.asset-manager :refer [play-sound]]
            [forge.effect :refer [applicable? handle]]
            [forge.world :refer [spawn-creature]]))

; "https://github.com/damn/core/issues/29"
(defmethods :effects/spawn
  (applicable? [_ {:keys [effect/source effect/target-position]}]
    (and (:entity/faction @source)
         target-position))

  (handle [[_ {:keys [property/id]}]
           {:keys [effect/source effect/target-position]}]
    (play-sound "bfxr_shield_consume")
    (spawn-creature {:position target-position
                     :creature-id id ; already properties/get called through one-to-one, now called again.
                     :components {:entity/fsm {:fsm :fsms/npc
                                               :initial-state :npc-idle}
                                  :entity/faction (:entity/faction @source)}})))
