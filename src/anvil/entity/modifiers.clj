(ns anvil.entity.modifiers
  (:refer-clojure :exclude [remove])
  (:require [anvil.component :as component]
            [anvil.operation :as op]
            [clojure.string :as str]
            [gdl.utils :refer [defmethods]]
            [gdl.malli :as m]))

(defmethods :entity/modifiers
  (component/info [[_ mods]]
    (when (seq mods)
      (str/join "\n" (keep (fn [[k ops]]
                             (op/info ops k)) mods)))))

(defn- mods-add    [mods other-mods] (merge-with op/add    mods other-mods))
(defn- mods-remove [mods other-mods] (merge-with op/remove mods other-mods))

(defn add    [entity mods] (update entity :entity/modifiers mods-add    mods))
(defn remove [entity mods] (update entity :entity/modifiers mods-remove mods))

(defn ->value [base-value {:keys [entity/modifiers]} modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (op/apply (modifier-k modifiers)
            base-value))

(defn- ->pos-int [val-max]
  (mapv #(-> % int (max 0)) val-max))

(defn apply-max-modifier [val-max entity modifier-k]
  {:pre  [(m/validate m/val-max-schema val-max)]
   :post [(m/validate m/val-max-schema val-max)]}
  (let [val-max (update val-max 1 ->value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [(min v mx) mx]))

(defn apply-min-modifier [val-max entity modifier-k]
  {:pre  [(m/validate m/val-max-schema val-max)]
   :post [(m/validate m/val-max-schema val-max)]}
  (let [val-max (update val-max 0 ->value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [v (max v mx)]))
