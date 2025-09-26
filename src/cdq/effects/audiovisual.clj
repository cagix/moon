(ns cdq.effects.audiovisual)

(defn applicable?[_ {:keys [effect/target-position]}]
  target-position)

(defn useful? [_ _effect-ctx _world]
  false)

(defn handle [[_ audiovisual] {:keys [effect/target-position]} _world]
  [[:tx/audiovisual target-position audiovisual]])
