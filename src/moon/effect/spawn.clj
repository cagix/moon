(ns moon.effect.spawn
  "🚧 Under construction ⚠️

  https://github.com/damn/core/issues/29"
  (:require [gdl.assets :refer [play-sound]]
            [moon.effect :as effect]
            [moon.world.entities :as entities]))

(defn applicable? [_]
  (and (:entity/faction @effect/source)
       effect/target-position))

(defn handle [{:keys [property/id]}]
  (play-sound "sounds/bfxr_shield_consume.wav")
  (entities/creature {:position effect/target-position
                      :creature-id id ; already properties/get called through one-to-one, now called again.
                      :components {:entity/fsm {:fsm :fsms/npc
                                                :initial-state :npc-idle}
                                   :entity/faction (:entity/faction @effect/source)}})
  nil)
