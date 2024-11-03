(ns moon.entity.modifiers
  (:require [clojure.string :as str]
            [moon.operations :as ops]))

(defn- dbg-info [mods]
  (str "\n [GRAY]"
       (binding [*print-level* nil]
         (with-out-str (clojure.pprint/pprint mods)))
       "[]"))

(defn info [[_ mods]]
  (when (seq mods)
    (str "[MODIFIERS]"
         (str/join "\n" (keep (fn [[k ops]] (ops/info ops k)) mods))
         "[]"
         (dbg-info mods))))

(defn- mods-add    [mods other-mods] (merge-with ops/add    mods other-mods))
(defn- mods-remove [mods other-mods] (merge-with ops/remove mods other-mods))

(defn handle [[k eid add-or-remove mods]]
  [[:e/assoc eid k ((case add-or-remove
                      :add    mods-add
                      :remove mods-remove) (k @eid) mods)]])

(defn- modifier-k [k]
  (keyword "modifier" (name k)))

(defn value
  ([entity k]
   (when-let [base-value (k entity)]
     (value entity (modifier-k k) base-value)))

  ([{:keys [entity/modifiers]} k base-value]
   {:pre [(= "modifier" (namespace k))]}
   (ops/apply (k modifiers)
              base-value)))
