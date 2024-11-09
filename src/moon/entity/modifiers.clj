(ns moon.entity.modifiers
  (:require [clojure.string :as str]
            [clojure.pprint]
            [gdl.system :refer [*k*]]
            [moon.operations :as ops]))

(defn- dbg-info [mods]
  (str "\n [GRAY]"
       (binding [*print-level* nil]
         (with-out-str (clojure.pprint/pprint mods)))
       "[]"))

(defn info [mods]
  (when (seq mods)
    (str "[MODIFIERS]"
         (str/join "\n" (keep (fn [[k ops]] (ops/info ops k)) mods))
         "[]"
         (dbg-info mods))))

(defn- mods-add    [mods other-mods] (merge-with ops/add    mods other-mods))
(defn- mods-remove [mods other-mods] (merge-with ops/remove mods other-mods))

(defn handle [eid add-or-remove mods]
  (swap! eid update *k* (case add-or-remove
                          :add    mods-add
                          :remove mods-remove) mods)
  nil)

(defn value [base-value {:keys [entity/modifiers]} modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (ops/apply (modifier-k modifiers)
             base-value))
