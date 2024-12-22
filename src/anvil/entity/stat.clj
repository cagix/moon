(ns anvil.entity.stat
  (:require [anvil.entity :as entity]
            [anvil.entity.modifiers :as mods]
            [gdl.utils :refer [defn-impl]]))

(defn-impl entity/stat [entity k]
  (when-let [base-value (k entity)]
    (mods/->value base-value
                  entity
                  (keyword "modifier" (name k)))))
