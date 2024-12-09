(ns forge.entity.stat
  (:require [anvil.entity :as entity]))

(defn ->value [entity k]
  (when-let [base-value (k entity)]
    (entity/mod-value base-value
                      entity
                      (keyword "modifier" (name k)))))
