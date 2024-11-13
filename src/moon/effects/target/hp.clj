(ns moon.effects.target.hp)

#_(defn- stat-k [effect-k]
    (keyword "stats" (name effect-k)))

#_(defn info [ops]
    (ops/info ops *k*))

#_(defn applicable? [_ {:keys [effect/source effect/target]}]
    (and effect/target
         (mods/value @target (stat-k *k*))))

#_(defn useful? [_ _]
    true)

#_(defn handle [operations {:keys [effect/source effect/target]}]
    (let [stat-k (stat-k *k*)]
      (when-let [value (mods/value @target stat-k)]
        (swap! target assoc stat-k (ops/apply operations value)))))
