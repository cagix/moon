(ns cdq.effects.target.audiovisual)

(defn applicable? [_ {:keys [effect/target]}]
  target)

(defn useful? [_ _effect-ctx _world]
  false)

(defn handle [[_ audiovisual] {:keys [effect/target]} _world]
  [[:tx/audiovisual (:body/position (:entity/body @target)) audiovisual]])
