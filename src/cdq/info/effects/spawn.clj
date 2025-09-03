(ns cdq.info.effects.spawn)

(defn info-segment [[_ {:keys [property/pretty-name]}] _ctx]
  (str "Spawns a " pretty-name))
