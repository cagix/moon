(ns moon.effect.spawn
  (:require [moon.component :as component]
            [moon.effect :as effect]))

; https://github.com/damn/core/issues/29
(defmethods :effect/spawn
  {:let {:keys [property/id]}}
  (component/applicable? [_]
    (and (:entity/faction @effect/source)
         effect/target-position))

  (component/handle [_]
    [[:tx/sound "sounds/bfxr_shield_consume.wav"]
     [:tx/creature {:position effect/target-position
                    :creature-id id ; already properties/get called through one-to-one, now called again.
                    :components {:entity/fsm {:fsm :fsms/npc
                                              :initial-state :npc-idle}
                                 :entity/faction (:entity/faction @effect/source)}}]]))
