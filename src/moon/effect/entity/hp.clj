(ns moon.effect.entity.hp)

#_(defn- stat-k [effect-k]
  (keyword "stats" (name effect-k)))

#_(defn info [ops]
  (ops/info ops *k*))

#_(defn applicable? [_]
  (and effect/target
       (mods/value @effect/target (stat-k *k*))))

#_(defn useful? [_]
  true)

#_(defn handle [operations]
  (let [stat-k (stat-k *k*)]
    (when-let [value (mods/value @effect/target stat-k)]
      [[:e/assoc effect/target stat-k (ops/apply operations value)]])))

; TODO make as tx also? from entity/mana itself? ( so can use at pay-mana/damage)
