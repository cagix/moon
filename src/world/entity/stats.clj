(ns world.entity.stats
  (:require [clojure.string :as str]
            [component.core :refer [defc defc*]]
            [component.info :as info]
            [component.tx :as tx]
            [data.operation :as op]
            [data.val-max :as val-max]
            [utils.core :refer [k->pretty-name]]
            [world.entity :as entity]
            [world.entity.hpbar :as hpbar]
            [world.entity.modifiers :refer [->modified-value]]
            [world.effect :as effect]))

(defn- defmodifier [k operations]
  (defc* k {:schema [:s/map-optional operations]}))

(defn-  stat-k->effect-k   [k] (keyword "effect.entity" (name k)))
(defn effect-k->stat-k     [k] (keyword "stats"         (name k)))
(defn-  stat-k->modifier-k [k] (keyword "modifier"      (name k)))

(defn entity-stat
  "Calculating value of the stat w. modifiers"
  [entity stat-k]
  (when-let [base-value (stat-k entity)]
    (->modified-value entity (stat-k->modifier-k stat-k) base-value)))

; is called :base/stat-effect so it doesn't show up in (:schema [:s/components-ns :effect.entity]) list in editor
; for :skill/effects
(defc :base/stat-effect
  (info/text [[k operations]]
    (str/join "\n"
              (for [operation operations]
                (str (op/info-text operation) " " (k->pretty-name k)))))

  (effect/applicable? [[k _]]
    (and effect/target
         (entity-stat @effect/target (effect-k->stat-k k))))

  (effect/useful? [_]
    true)

  (tx/handle [[effect-k operations]]
    (let [stat-k (effect-k->stat-k effect-k)]
      (when-let [effective-value (entity-stat @effect/target stat-k)]
        [[:e/assoc effect/target stat-k
          ; TODO similar to components.entity.modifiers/->modified-value
          ; but operations not sort-by op/order ??
          ; op-apply reuse fn over operations to get effectiv value
          (reduce (fn [value operation]
                    (op/apply operation value))
                  effective-value
                  operations)]]))))

(defn defstat [k {:keys [modifier-ops effect-ops] :as attr-m}]
  (defc* k attr-m)
  (derive k :entity/stat)
  (when modifier-ops
    (defmodifier (stat-k->modifier-k k) modifier-ops))
  (when effect-ops
    (let [effect-k (stat-k->effect-k k)]
      (defc* effect-k {:schema [:s/map-optional effect-ops]})
      (derive effect-k :base/stat-effect))))

(load "stats_impl")

(defc :entity/stat
  (info/text [[k v]]
    (str (k->pretty-name k) ": " (entity-stat info/*info-text-entity* k))))
