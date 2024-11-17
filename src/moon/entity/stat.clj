(ns moon.entity.stat
  (:require [moon.entity.modifiers :as mods]))

(defn value [entity k]
  (when-let [base-value (k entity)]
    (mods/value base-value
                entity
                (keyword "modifier" (name k)))))
