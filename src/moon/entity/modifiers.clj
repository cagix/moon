(ns ^:no-doc moon.entity.modifiers
  (:require [clojure.string :as str]
            [gdl.graphics.color :as color]
            [gdl.utils :refer [k->pretty-name]]
            [moon.component :refer [defc defc*] :as component]
            [moon.entity :as entity]
            [moon.effect :as effect]
            [moon.graphics :as g]
            [moon.modifiers :as mods]
            [moon.operation :as op]
            [moon.val-max :as val-max]))

(color/put "MODIFIER_BLUE" :cyan)

(defc :entity/modifiers
  {:schema [:s/components-ns :modifier]
   :let modifiers}
  (entity/->v [_]
    (into {} (for [[modifier-k operations] modifiers]
               [modifier-k (into {} (for [[operation-k value] operations]
                                      [operation-k [value]]))])))

  (component/info [_]
    (let [modifiers (mods/sum-operation-values modifiers)]
      (when (seq modifiers)
        (mods/info-text modifiers))))

  (component/handle [[k eid add-or-remove mods]]
    [[:e/assoc eid k ((case add-or-remove
                        :add    mods/add
                        :remove mods/remove) (k @eid) mods)]]))

(defc :item/modifiers
  {:schema [:s/components-ns :modifier]
   :let modifiers}
  (component/info [_]
    (when (seq modifiers)
      (mods/info-text modifiers))))

(defn- ops-apply [ops value]
  (reduce (fn [value op]
            (op/apply op value))
          value
          (sort-by op/order ops)))

(defn- modified-value [{:keys [entity/modifiers]} modifier-k base-value]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (ops-apply (->> modifiers
                  modifier-k
                  mods/sum-ops)
             base-value))

(.bindRoot #'entity/modified-value modified-value)

(defn- effect-k   [stat-k]   (keyword "effect.entity" (name stat-k)))
(defn- stat-k     [effect-k] (keyword "stats"         (name effect-k)))
(defn- modifier-k [stat-k]   (keyword "modifier"      (name stat-k)))

(defn- entity-stat [entity stat-k]
  (when-let [base-value (stat-k entity)]
    (modified-value entity
                    (modifier-k stat-k)
                    base-value)))

(.bindRoot #'entity/stat entity-stat)

(defc :base/stat-effect
  (component/info [[k operations]]
    (str/join "\n"
              (for [operation operations]
                (str (mods/op-info-text operation) " " (k->pretty-name k)))))

  (component/applicable? [[k _]]
    (and effect/target
         (entity/stat @effect/target (stat-k k))))

  (component/useful? [_]
    true)

  (component/handle [[effect-k operations]]
    (let [stat-k (stat-k effect-k)]
      (when-let [effective-value (entity/stat @effect/target stat-k)]
        [[:e/assoc effect/target stat-k (ops-apply operations
                                                   effective-value)]]))))

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

; TODO
; @ hp says here 'Minimum' hp instead of just 'HP'
; Sets to 0 but don't kills
; Could even set to a specific value ->
; op/set-to-ratio 0.5 ....
; sets the hp to 50%...
(defstat :stats/hp
  {:schema pos-int?
   :modifier-ops [:op/max-inc :op/max-mult]
   :effect-ops [:op/val-inc :op/val-mult :op/max-inc :op/max-mult]})

(def ^:private hpbar-colors
  {:green     [0 0.8 0]
   :darkgreen [0 0.5 0]
   :yellow    [0.5 0.5 0]
   :red       [0.5 0 0]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
                (> ratio 0.75) :green
                (> ratio 0.5)  :darkgreen
                (> ratio 0.25) :yellow
                :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [{:keys [position width half-width half-height]}
                   ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (g/pixels->world-units 5)
          border (g/pixels->world-units borders-px)]
      (g/draw-filled-rectangle x y width height :black)
      (g/draw-filled-rectangle (+ x border)
                               (+ y border)
                               (- (* width ratio) (* 2 border))
                               (- height (* 2 border))
                               (hpbar-color ratio)))))

(defc :stats/hp
  (entity/->v [[_ v]]
    [v v])

  (entity/render-info [_ entity]
    (let [ratio (val-max/ratio (entity/stat entity :stats/hp))]
      (when (or (< ratio 1) (:entity/mouseover? entity))
        (draw-hpbar entity ratio)))))

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
