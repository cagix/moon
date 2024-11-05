(ns moon.effect.spawn
  "🚧 Under construction ⚠️

  https://github.com/damn/core/issues/29"
  (:require [moon.effect :as effect]))

(defn applicable? [_]
  (and (:entity/faction @effect/source)
       effect/target-position))

(defn handle [{:keys [property/id]}]
  [[:tx/sound "sounds/bfxr_shield_consume.wav"]
   [:tx/creature {:position effect/target-position
                  :creature-id id ; already properties/get called through one-to-one, now called again.
                  :components {:entity/fsm {:fsm :fsms/npc
                                            :initial-state :npc-idle}
                               :entity/faction (:entity/faction @effect/source)}}]])
