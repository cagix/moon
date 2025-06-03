(ns cdq.effects.spawn
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :effects/spawn
  (effect/applicable? [_ {:keys [effect/source effect/target-position]}]
    (and (entity/faction @source)
         target-position))

  (effect/handle [[_ {:keys [property/id]}]
                  {:keys [effect/source effect/target-position]}
                  _ctx]
    [[:tx/spawn-creature {:position target-position
                          :creature-id id ; already properties/get called through one-to-one, now called again.
                          :components {:entity/fsm {:fsm :fsms/npc
                                                    :initial-state :npc-idle}
                                       :entity/faction (entity/faction @source)}}]]))

(comment
 ; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
 ; same code as in render entities on world view screens/world
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [targets (creatures-in-los-of-player)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (entity/position @%)) targets)))

 )
