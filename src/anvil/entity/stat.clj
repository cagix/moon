(ns anvil.entity.stat
  (:require [anvil.entity :as entity]
            [anvil.entity.modifiers :as mods]))

(defn-impl entity/stat [entity k]
  (when-let [base-value (k entity)]
    (mods/->value base-value
                  entity
                  (keyword "modifier" (name k)))))
