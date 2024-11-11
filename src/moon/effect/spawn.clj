(ns moon.effect.spawn
  "🚧 Under construction ⚠️

  https://github.com/damn/core/issues/29"
  (:require [gdl.assets :refer [play-sound]]
            [moon.effects :refer [source target target-position]]
            [moon.world.entities :as entities]))

(defn applicable? [_]
  (and (:entity/faction @source)
       target-position))

(defn handle [{:keys [property/id]}]
  (play-sound "sounds/bfxr_shield_consume.wav")
  (entities/creature {:position target-position
                      :creature-id id ; already properties/get called through one-to-one, now called again.
                      :components {:entity/fsm {:fsm :fsms/npc
                                                :initial-state :npc-idle}
                                   :entity/faction (:entity/faction @source)}}))
