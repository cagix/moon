(ns cdq.effects.target.damage)

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       #_(:entity/hp @target))) ; not exist anymore ... bugfix .... -> is 'creature?'

(defn handle [[_ damage]
              {:keys [effect/source effect/target]}
              _world]
  [[:tx/deal-damage source target damage]])
