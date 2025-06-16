(ns cdq.effects.sound)

(defn applicable? [_ _ctx]
  true)

(defn useful? [_ _effect-ctx _world]
  false)

(defn handle [[_ sound] _effect-ctx _world]
  [[:tx/sound sound]])
