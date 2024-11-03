(ns moon.modifiers
  (:refer-clojure :exclude [remove])
  (:require [clojure.string :as str]
            [gdl.utils :refer [mapvals k->pretty-name]]
            [moon.component :as component :refer [defmethods]]
            [moon.entity :as entity]
            [moon.effect :as effect]
            [moon.operations :as ops]))

(defn add    [mods other-mods] (merge-with ops/add    mods other-mods))
(defn remove [mods other-mods] (merge-with ops/remove mods other-mods))

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

(defn- effect-k   [stat-k]   (keyword "effect.entity" (name stat-k)))
(defn- stat-k     [effect-k] (keyword "stats"         (name effect-k)))
(defn- modifier-k [stat-k]   (keyword "modifier"      (name stat-k)))

(defn effective-value
  ([entity stat-k]
   (when-let [base-value (stat-k entity)]
     (effective-value entity (modifier-k stat-k) base-value)))

  ([{:keys [entity/modifiers]} modifier-k base-value]
   {:pre [(= "modifier" (namespace modifier-k))]}
   (ops/apply (modifier-k modifiers)
              base-value)))

(defmethods :base/stat-effect
  (component/info [[k ops]]
    (ops/info ops k))

  (component/applicable? [[k _]]
    (and effect/target
         (effective-value @effect/target (stat-k k))))

  (component/useful? [_]
    true)

  (component/handle [[effect-k operations]]
    (let [stat-k (stat-k effect-k)]
      (when-let [effective-value (effective-value @effect/target stat-k)]
        [[:e/assoc effect/target stat-k (ops/apply operations effective-value)]]))))

(defmethod component/info :entity/stat [[k v]]
  (str (k->pretty-name k) ": " (effective-value component/*info-text-entity* k)))

(defn defstat [k]
  {:pre [(= (namespace k) "stats")]}
  (derive k :entity/stat)
  (derive (effect-k k) :base/stat-effect))

; TODO negate this value also @ use (modifier damage receive)
; so can make positiive modifeirs green , negative red....

; TODO needs to be there for each npc - make non-removable (added to all creatures)
; and no need at created player (npc controller component?)
(defstat :stats/aggro-range)
(defstat :stats/reaction-time)
(defstat :stats/hp)
(defstat :stats/mana)

(defmethod entity/->v :stats/mana [[_ v]]
  [v v])

(defmethod component/handle :tx.entity.stats/pay-mana-cost [[_ eid cost]]
  (let [mana-val ((effective-value @eid :stats/mana) 0)]
    (assert (<= cost mana-val))
    [[:e/assoc-in eid [:stats/mana 0] (- mana-val cost)]]))

(comment
 (let [mana-val 4
       eid (atom (entity/map->Entity {:stats/mana [mana-val 10]}))
       mana-cost 3
       resulting-mana (- mana-val mana-cost)]
   (= (component/handle [:tx.entity.stats/pay-mana-cost eid mana-cost] nil)
      [[:e/assoc-in eid [:stats/mana 0] resulting-mana]]))
 )

; * TODO clamp/post-process effective-values @ stat-k->effective-value
; * just don't create movement-speed increases too much?
; * dont remove strength <0 or floating point modifiers  (op/int-inc ?)
; * cast/attack speed dont decrease below 0 ??

; TODO clamp between 0 and max-speed ( same as movement-speed-schema )
(defstat :stats/movement-speed) ;(m/form entity/movement-speed-schema)

; TODO show the stat in different color red/green if it was permanently modified ?
; or an icon even on the creature
; also we want audiovisuals always ...
(derive :effect.entity/movement-speed :base/stat-effect)
; TODO is not automatically derived?

; TODO clamp into ->pos-int
(defstat :stats/strength)

; TODO here >0
(comment
 (let [doc "action-time divided by this stat when a skill is being used.
           Default value 1.

           For example:
           attack/cast-speed 1.5 => (/ action-time 1.5) => 150% attackspeed."]))
(defstat :stats/cast-speed)
(defstat :stats/attack-speed)

; TODO bounds
(defstat :stats/armor-save)
(defstat :stats/armor-pierce)
