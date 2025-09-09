(ns cdq.effects.sound)

(defn applicable? [_ _ctx] true)
(defn useful? [_ _effect-ctx _ctx] false)
(defn handle [[_ sound] _effect-ctx _ctx]
  [[:tx/sound sound]])
