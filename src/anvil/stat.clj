(ns anvil.stat
  (:require [anvil.modifiers :as mods]))

(defn ->value [entity k]
  (when-let [base-value (k entity)]
    (mods/->value base-value
                  entity
                  (keyword "modifier" (name k)))))
