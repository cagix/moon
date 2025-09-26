(ns cdq.effects.spawn)

(defn applicable? [_ {:keys [effect/source effect/target-position]}]
  (and (:entity/faction @source)
       target-position))

(defn handle [[_ {:keys [property/id] :as property}]
              {:keys [effect/source effect/target-position]}
              _world]
  [[:tx/spawn-creature {:position target-position
                        :creature-property property
                        :components {:entity/fsm {:fsm :fsms/npc
                                                  :initial-state :npc-idle}
                                     :entity/faction (:entity/faction @source)}}]])

(defn info-text [[_ {:keys [property/pretty-name]}] _world]
  (str "Spawns a " pretty-name))
