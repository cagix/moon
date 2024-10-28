(ns moon.entity.modifiers
  (:require [gdl.utils :refer [k->pretty-name]]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.effect :as effect]
            [moon.modifiers :as mods]
            [moon.operations :as ops]))

(defc :entity/modifiers
  {:schema [:s/components-ns :modifier]}
  (entity/->v [[_ value-mods]]
    (mods/value-mods->mods value-mods))

  (component/info [[_ mods]]
    (mods/info-text (mods/sum-vals mods)))

  (component/handle [[k eid add-or-remove mods]]
    [[:e/assoc eid k ((case add-or-remove
                        :add    mods/add
                        :remove mods/remove) (k @eid) mods)]]))

(defn- modified-value [{:keys [entity/modifiers]} modifier-k base-value]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (ops/apply (->> modifiers modifier-k ops/sum-vals)
             base-value))

(bind-root #'entity/modified-value modified-value)

(defn- effect-k   [stat-k]   (keyword "effect.entity" (name stat-k)))
(defn- stat-k     [effect-k] (keyword "stats"         (name effect-k)))
(defn- modifier-k [stat-k]   (keyword "modifier"      (name stat-k)))

(defn- entity-stat [entity stat-k]
  (when-let [base-value (stat-k entity)]
    (modified-value entity (modifier-k stat-k) base-value)))

(bind-root #'entity/stat entity-stat)

; namespace 'base' so doesnt show up as 'effect' or 'effect.entity' ...
; just add :show-in-ui? false to attr-map !
(defc :base/stat-effect
  (component/info [[k ops]]
    (ops/info-text ops k))

  (component/applicable? [[k _]]
    (and effect/target
         (entity/stat @effect/target (stat-k k))))

  (component/useful? [_]
    true)

  (component/handle [[effect-k operations]]
    (let [stat-k (stat-k effect-k)]
      (when-let [effective-value (entity/stat @effect/target stat-k)]
        [[:e/assoc effect/target stat-k (ops/apply operations effective-value)]]))))

(defn defmodifier [k operations]
  {:pre [(= (namespace k) "modifier")]}
  (defc* k {:schema [:s/map-optional operations]}))

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
    (str (k->pretty-name k) ": " (entity/stat component/*info-text-entity* k))))

; TODO negate this value also @ use
; so can make positiive modifeirs green , negative red....
(defmodifier :modifier/damage-receive [:op/max-inc :op/max-mult])
(defmodifier :modifier/damage-deal [:op/val-inc :op/val-mult :op/max-inc :op/max-mult])

; TODO needs to be there for each npc - make non-removable (added to all creatures)
; and no need at created player (npc controller component?)
(defstat :stats/aggro-range   {:schema nat-int?})
(defstat :stats/reaction-time {:schema pos-int?})

(defstat :stats/mana
  {:schema nat-int?
   :modifier-ops [:op/max-inc :op/max-mult]
   :effect-ops [:op/val-inc :op/val-mult :op/max-inc :op/max-mult]})

(defc :stats/mana
  (entity/->v [[_ v]]
    [v v]))

(defc :tx.entity.stats/pay-mana-cost
  (component/handle [[_ eid cost]]
    (let [mana-val ((entity/stat @eid :stats/mana) 0)]
      (assert (<= cost mana-val))
      [[:e/assoc-in eid [:stats/mana 0] (- mana-val cost)]])))

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
(defstat :stats/movement-speed
  {:schema pos? ;(m/form entity/movement-speed-schema)
   :modifier-ops [:op/inc :op/mult]})

; TODO show the stat in different color red/green if it was permanently modified ?
; or an icon even on the creature
; also we want audiovisuals always ...
(defc :effect.entity/movement-speed
  {:schema [:s/map [:op/mult]]})
(derive :effect.entity/movement-speed :base/stat-effect)

; TODO clamp into ->pos-int
(defstat :stats/strength
  {:schema nat-int?
   :modifier-ops [:op/inc]})

; TODO here >0
(let [doc "action-time divided by this stat when a skill is being used.
          Default value 1.

          For example:
          attack/cast-speed 1.5 => (/ action-time 1.5) => 150% attackspeed."
      schema pos?
      operations [:op/inc]]
  (defstat :stats/cast-speed
    {:schema schema
     :editor/doc doc
     :modifier-ops operations})

  (defstat :stats/attack-speed
    {:schema schema
     :editor/doc doc
     :modifier-ops operations}))

; TODO bounds
(defstat :stats/armor-save
  {:schema number?
   :modifier-ops [:op/inc]})

(defstat :stats/armor-pierce
  {:schema number?
   :modifier-ops [:op/inc]})
