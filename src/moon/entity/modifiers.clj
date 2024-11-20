(ns moon.entity.modifiers
  (:require [clojure.string :as str]
            [clojure.pprint]
            [moon.system :refer [*k*]]
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
