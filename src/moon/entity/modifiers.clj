(ns ^:no-doc moon.entity.modifiers
  (:require [clojure.string :as str]
            [gdl.graphics.color :as color]
            [gdl.utils :refer [safe-remove-one update-kv k->pretty-name]]
            [moon.component :refer [defc defc*] :as component]
            [moon.info :as info]
            [moon.operation :as op]
            [moon.entity :as entity]
            [moon.effect :as effect]))

(defn- ops-add    [ops value-ops] (update-kv conj            ops value-ops))
(defn- ops-remove [ops value-ops] (update-kv safe-remove-one ops value-ops))

(comment
 (= (ops-add {:+ [1 2 3]}
             {:* -0.5 :+ -1})
    {:+ [1 2 3 -1], :* [-0.5]})

 (= (ops-remove {:+ [1 2 3] :* [-0.5]}
                {:+ 2 :* -0.5})
    {:+ [1 3], :* []})
 )

(defn- mods-add    [mods value-mods] (update-kv ops-add    mods value-mods))
(defn- mods-remove [mods value-mods] (update-kv ops-remove mods value-mods))

(comment
 (= (mods-add {:speed {:+ [1 2 3]}}
              {:speed {:* -0.5}})
    {:speed {:+ [1 2 3], :* [-0.5]}})

 (= (mods-remove {:speed {:+ [1 2 3] :* [-0.5]}}
                 {:speed {:+ 2 :* -0.5}})
    {:speed {:+ [1 3], :* []}})
 )

(comment
 ; Example blood-helm
 {:modifier/hp {:op/max-inc -200}}

 ; :entity/hp => {:base-value ... :operations ... }
 ; modifier: tuple of [component operation(s)]
 )

(defn update-mods [[_ eid mods] f]
  [[:e/update eid :entity/modifiers #(f % mods)]])

(defc :tx/apply-modifiers   (component/handle [this] (update-mods this mods-add)))
(defc :tx/reverse-modifiers (component/handle [this] (update-mods this mods-remove)))

; DRY ->effective-value (summing)
; also: sort-by op/order @ modifier/info-text itself (so player will see applied order)
(defn- sum-operation-values [modifiers]
  (for [[modifier-k operations] modifiers
        :let [operations (for [[operation-k values] operations
                               :let [value (apply + values)]
                               :when (not (zero? value))]
                           [operation-k value])]
        :when (seq operations)]
    [modifier-k operations]))

(color/put "MODIFIER_BLUE" :cyan)

; For now no green/red color for positive/negative numbers
; as :stats/damage-receive negative value would be red but actually a useful buff
; -> could give damage reduce 10% like in diablo 2
; and then make it negative .... @ applicator
(def ^:private positive-modifier-color "[MODIFIER_BLUE]" #_"[LIME]")
(def ^:private negative-modifier-color "[MODIFIER_BLUE]" #_"[SCARLET]")

(defn mod-info-text [modifiers]
  (str "[MODIFIER_BLUE]"
       (str/join "\n"
                 (for [[modifier-k operations] modifiers
                       operation operations]
                   (str (op/info-text operation) " " (k->pretty-name modifier-k))))
       "[]"))

(defc :entity/modifiers
  {:schema [:s/components-ns :modifier]
   :let modifiers}
  (entity/->v [_]
    (into {} (for [[modifier-k operations] modifiers]
               [modifier-k (into {} (for [[operation-k value] operations]
                                      [operation-k [value]]))])))

  (component/info [_]
    (let [modifiers (sum-operation-values modifiers)]
      (when (seq modifiers)
        (mod-info-text modifiers)))))

(defn modified-value [{:keys [entity/modifiers]} modifier-k base-value]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (->> modifiers
       modifier-k
       (sort-by op/order)
       (reduce (fn [base-value [operation-k values]]
                 (op/apply [operation-k (apply + values)] base-value))
               base-value)))

(defn defmodifier [k operations]
  {:pre [(= (namespace k) "modifier")]}
  (defc* k {:schema [:s/map-optional operations]}))

(defn- effect-k   [stat-k]   (keyword "effect.entity" (name stat-k)))
(defn- stat-k     [effect-k] (keyword "stats"         (name effect-k)))
(defn- modifier-k [stat-k]   (keyword "modifier"      (name stat-k)))

(defn entity-stat [entity stat-k]
  (when-let [base-value (stat-k entity)]
    (modified-value entity
                    (modifier-k stat-k)
                    base-value)))

(defc :base/stat-effect
  (component/info [[k operations]]
    (str/join "\n"
              (for [operation operations]
                (str (op/info-text operation) " " (k->pretty-name k)))))

  (effect/applicable? [[k _]]
    (and effect/target
         (entity-stat @effect/target (stat-k k))))

  (effect/useful? [_]
    true)

  (component/handle [[effect-k operations]]
    (let [stat-k (stat-k effect-k)]
      (when-let [effective-value (entity-stat @effect/target stat-k)]
        [[:e/assoc effect/target stat-k
          (reduce (fn [value operation]
                    (op/apply operation value))
                  effective-value
                  operations)]]))))

(defn defstat [k {:keys [modifier-ops effect-ops] :as attr-m}]
  {:pre [(= (namespace k) "stats")]}
  (defc* k attr-m)
  (derive k :entity/stat)
  (when modifier-ops
    (defmodifier (modifier-k k) modifier-ops))
  (when effect-ops
    (let [effect-k (effect-k k)]
      (defc* effect-k {:schema [:s/map-optional effect-ops]})
      (derive effect-k :base/stat-effect))))

(defc :entity/stat
  (component/info [[k v]]
    (str (k->pretty-name k) ": " (entity-stat info/*info-text-entity* k))))
