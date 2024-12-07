(ns forge.effects.target.hp
  (:require [clojure.utils :refer [defmethods]]
            [forge.effect :refer [applicable? handle]]))

#_(defn- stat-k [effect-k]
    (keyword "stats" (name effect-k)))

#_(defmethods :effects.target/hp
  (info [[k ops]]
    (ops/info ops k))

  (applicable? [[k _] {:keys [effect/source effect/target]}]
    (and effect/target
         (mods/value @target (stat-k k))))

  (useful? [_ _]
    true)

  (handle [[k operations] {:keys [effect/source effect/target]}]
    (let [stat-k (stat-k k)]
      (when-let [value (mods/value @target stat-k)]
        (swap! target assoc stat-k (ops/apply operations value))))))
