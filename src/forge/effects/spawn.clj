(ns forge.effects.spawn
  (:require [forge.app.asset-manager :refer [play-sound]]
            [forge.world :refer [spawn-creature]]))

; "https://github.com/damn/core/issues/29"
(defn applicable? [_ {:keys [effect/source effect/target-position]}]
  (and (:entity/faction @source)
       target-position))

(defn handle [[_ {:keys [property/id]}]
              {:keys [effect/source effect/target-position]}]
  (play-sound "bfxr_shield_consume")
  (spawn-creature {:position target-position
                   :creature-id id ; already properties/get called through one-to-one, now called again.
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-idle}
                                :entity/faction (:entity/faction @source)}}))
