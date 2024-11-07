(ns moon.entity.stat
  (:require [gdl.system :refer [*k*]]
            [gdl.utils :refer [k->pretty-name]]
            [moon.component :as component]
            [moon.entity.modifiers :as mods]))

(defn value [entity k]
  (when-let [base-value (k entity)]
    (mods/value base-value
                entity
                (keyword "modifier" (name k)))))

(defn info [_]
  (str (k->pretty-name *k*)
       ": "
       (value component/*info-text-entity* *k*)))
