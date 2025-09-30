; Use schema, pre/post, tests for understanding.
; e.g. ops just :ops/inc/:ops/mult?
(ns cdq.entity.stats
  (:require [cdq.malli :as m]
            [cdq.stats.ops :as ops]
            [cdq.val-max :as val-max]
            [clojure.string :as str]))

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

(defn get-stat-value [stats stat-k]
  (when-let [base-value (stat-k stats)]
    (get-value base-value
               (:stats/modifiers stats)
               (keyword "modifier" (name stat-k)))))

(defn add    [stats mods] (update stats :stats/modifiers add*    mods))
(defn remove-mods [stats mods] (update stats :stats/modifiers remove* mods))

(defn get-mana
  [{:keys [stats/mana
           stats/modifiers]}]
  (apply-max mana modifiers :modifier/mana-max))

(defn mana-val [stats]
  (if (:stats/mana stats)
    ((get-mana stats) 0) ; TODO fucking optional shit
    0))

(defn not-enough-mana? [stats {:keys [skill/cost]}]
  (and cost (> cost (mana-val stats))))

(defn pay-mana-cost [stats cost]
  (let [mana-val (mana-val stats)]
    (assert (<= cost mana-val))
    (assoc-in stats [:stats/mana 0] (- mana-val cost))))

(defn get-hitpoints
  [{:keys [stats/hp
           stats/modifiers]}]
  (apply-max hp modifiers :modifier/hp-max))

(defn create [stats _world]
  (-> (if (:stats/mana stats)
        (update stats :stats/mana (fn [v] [v v]))
        stats)
      (update :stats/hp   (fn [v] [v v])))

  #_(-> stats
        (update :stats/mana (fn [v] [v v])) ; TODO is OPTIONAL ! then making [nil nil]
        (update :stats/hp   (fn [v] [v v]))))

(def ^:private non-val-max-stat-ks
  [:stats/movement-speed
   :stats/aggro-range
   :stats/reaction-time
   :stats/strength
   :stats/cast-speed
   :stats/attack-speed
   :stats/armor-save
   :stats/armor-pierce])

(defn info-text [[_ stats] _world]
  (str/join "\n" (concat
                  ["*STATS*"
                   (str "Mana: " (if (:stats/mana stats)
                                   (get-mana stats)
                                   "-"))
                   (str "Hitpoints: " (get-hitpoints stats))]
                  (for [stat-k non-val-max-stat-ks]
                    (str (str/capitalize (name stat-k)) ": "
                         (get-stat-value stats stat-k))))))
