(ns moon.effect.spawn
  (:require [moon.component :refer [defc] :as component]
            [moon.effect :as effect]))

; https://github.com/damn/core/issues/29
(defc :effect/spawn
  {:schema [:s/one-to-one :properties/creatures]
   :let {:keys [property/id]}}
  (effect/applicable? [_]
    (and (:entity/faction @effect/source)
         effect/target-position))

  (component/handle [_]
    [[:tx/sound "sounds/bfxr_shield_consume.wav"]
     [:tx/creature {:position effect/target-position
                    :creature-id id ; already properties/get called through one-to-one, now called again.
                    :components {:entity/fsm {:fsm :fsms/npc
                                              :initial-state :npc-idle}
                                 :entity/faction (:entity/faction @effect/source)}}]]))
