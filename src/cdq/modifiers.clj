(ns cdq.modifiers
  (:refer-clojure :exclude [remove])
  (:require [cdq.op :as op]))

(defn get-value [base-value modifiers modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (op/apply (modifier-k modifiers)
            base-value))

(defn add    [mods other-mods] (merge-with op/add    mods other-mods))
(defn remove [mods other-mods] (merge-with op/remove mods other-mods))
