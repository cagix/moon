(ns forge.entity.modifiers
  (:refer-clojure :exclude [remove])
  (:require [forge.ops :as ops]))

(defn- mods-add    [mods other-mods] (merge-with ops/add    mods other-mods))
(defn- mods-remove [mods other-mods] (merge-with ops/remove mods other-mods))

(defn add    [entity mods] (update entity :entity/modifiers mods-add    mods))
(defn remove [entity mods] (update entity :entity/modifiers mods-remove mods))

(defn ->value [base-value {:keys [entity/modifiers]} modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (ops/apply (modifier-k modifiers)
             base-value))
