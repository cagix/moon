; Use schema, pre/post, tests for understanding.
; e.g. ops just :ops/inc/:ops/mult?
(ns cdq.entity.stats
  (:require [cdq.malli :as m]
            [cdq.stats :as stats]
            [cdq.stats.ops :as ops]
            [cdq.val-max :as val-max]
            [com.badlogic.gdx.graphics.color :as color]))

(defn- get-value [base-value modifiers modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (ops/apply (modifier-k modifiers)
             base-value))

(defn- ->pos-int [val-max]
  (mapv #(-> % int (max 0)) val-max))

; TODO can just pass ops instead of modifiers modifier-k
(defn apply-max [val-max modifiers modifier-k]
  (assert (m/validate val-max/schema val-max) val-max)
  (let [val-max (update val-max 1 get-value modifiers modifier-k)
        [v mx] (->pos-int val-max)
        result [(min v mx) mx]]
  (assert (m/validate val-max/schema result) result)
  result))

; TODO can just pass ops instead of modifiers modifier-k
(defn apply-min [val-max modifiers modifier-k]
  (assert (m/validate val-max/schema val-max) val-max)
  (let [val-max (update val-max 0 get-value modifiers modifier-k)
        [v mx] (->pos-int val-max)
        result [v (max v mx)]]
    (assert (m/validate val-max/schema result) result)
    result))

(defn- add*    [mods other-mods] (merge-with ops/add    mods other-mods))
(defn- remove* [mods other-mods] (merge-with ops/remove mods other-mods))

; 1. name ! :entity/ -> :stats/
; 2. tests/protocols -> what are data structure of modifiers => is stat-k
; witha modifier key????
; how does the whole thing look like
; including editor based omgfwtf

(defrecord Stats []
  stats/Stats
  (get-stat-value [stats stat-k]
    (when-let [base-value (stat-k stats)]
      (get-value base-value
                 (:entity/modifiers stats)
                 (keyword "modifier" (name stat-k)))))

  (add    [stats mods] (update stats :entity/modifiers add*    mods))
  (remove-mods [stats mods] (update stats :entity/modifiers remove* mods))

  (get-mana
    [{:keys [entity/mana
             entity/modifiers]}]
    (apply-max mana modifiers :modifier/mana-max))

  (mana-val [stats]
    (if (:entity/mana stats)
      ((stats/get-mana stats) 0) ; TODO fucking optional shit
      0))

  (not-enough-mana? [stats {:keys [skill/cost]}]
    (and cost (> cost (stats/mana-val stats))))

  (pay-mana-cost [stats cost]
    (let [mana-val (stats/mana-val stats)]
      (assert (<= cost mana-val))
      (assoc-in stats [:entity/mana 0] (- mana-val cost))))

  (get-hitpoints
    [{:keys [entity/hp
             entity/modifiers]}]
    (apply-max hp modifiers :modifier/hp-max)))

(defn create [stats _world]
  (map->Stats (-> (if (:entity/mana stats)
                    (update stats :entity/mana (fn [v] [v v]))
                    stats)
                  (update :entity/hp   (fn [v] [v v]))))

  #_(-> stats
        (update :entity/mana (fn [v] [v v])) ; TODO is OPTIONAL ! then making [nil nil]
        (update :entity/hp   (fn [v] [v v]))))
