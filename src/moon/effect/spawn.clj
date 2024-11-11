(ns moon.effect.spawn
  "🚧 Under construction ⚠️

  https://github.com/damn/core/issues/29"
  (:require [gdl.assets :refer [play-sound]]
            [moon.world.entities :as entities]))

(defn applicable? [_ {:keys [effect/source effect/target-position]}]
  (and (:entity/faction @source)
       target-position))

(defn handle [{:keys [property/id]}
              {:keys [effect/source effect/target-position]}]
  (play-sound "sounds/bfxr_shield_consume.wav")
  (entities/creature {:position target-position
                      :creature-id id ; already properties/get called through one-to-one, now called again.
                      :components {:entity/fsm {:fsm :fsms/npc
                                                :initial-state :npc-idle}
                                   :entity/faction (:entity/faction @source)}}))
