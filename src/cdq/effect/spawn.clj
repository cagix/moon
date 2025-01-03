(ns cdq.effect.spawn
  (:require [cdq.context :as world]))

(defn text [[_ {:keys [property/pretty-name]}] _entity _context]
  (str "Spawns a " pretty-name))

; "https://github.com/damn/core/issues/29"
(defn applicable? [_ {:keys [effect/source effect/target-position]}]
  (and (:entity/faction @source)
       target-position))

(defn handle [[_ {:keys [property/id]}]
              {:keys [effect/source effect/target-position]}
              c]
  (world/creature c
                  {:position target-position
                   :creature-id id ; already properties/get called through one-to-one, now called again.
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-idle}
                                :entity/faction (:entity/faction @source)}}))
