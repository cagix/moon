(ns moon.entity.modifiers
  (:require [moon.modifiers :as mods]
            [moon.operations :as ops]))

(defn ->v [[_ value-mods]]
  (mods/value-mods->mods value-mods))

(defn info [[_ mods]]
  (mods/info-text (mods/sum-vals mods)))

(defn handle [[k eid add-or-remove mods]]
  [[:e/assoc eid k ((case add-or-remove
                      :add    mods/add
                      :remove mods/remove) (k @eid) mods)]])

(defn effective-value [{:keys [entity/modifiers]} modifier-k base-value]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (ops/apply (->> modifiers modifier-k ops/sum-vals)
             base-value))
