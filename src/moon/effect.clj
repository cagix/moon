(ns moon.effect
  (:require [moon.component :refer [defsystem]]
            [moon.entity :as entity]))

(defsystem applicable?
  "An effect will only be done (with component/handle) if this function returns truthy.
Required system for every effect, no default.")

(defsystem useful?
  "Used for NPC AI.
Called only if applicable? is truthy.
For example use for healing effect is only useful if hitpoints is < max.
Default method returns true.")
(defmethod useful? :default [_] true)

(defsystem render!  "Renders effect during active-skill state while active till done?. Default do nothing.")
(defmethod render! :default [_])

(defn filter-applicable? [effect]
  (filter applicable? effect))

(defn effect-applicable? [effect]
  (seq (filter-applicable? effect)))

(defn effect-useful? [effect]
  (->> effect
       filter-applicable?
       (some useful?)))

; SCHEMA effect-ctx
; * source = always available
; # npc:
;   * target = maybe
;   * direction = maybe
; # player
;  * target = maybe
;  * target-position  = always available (mouse world position)
;  * direction  = always available (from mouse world position)
(declare ^:dynamic source
         ^:dynamic target
         ^:dynamic target-direction
         ^:dynamic target-position)

(defmacro with-ctx [ctx & body]
  `(binding [source           (:effect/source           ~ctx)
             target           (:effect/target           ~ctx)
             target-direction (:effect/target-direction ~ctx)
             target-position  (:effect/target-position  ~ctx)]
     ~@body))

(defn- mana-value [entity]
  (if-let [mana (entity/stat entity :stats/mana)]
    (mana 0)
    0))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (> cost (mana-value entity)))

(defn skill-usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill}]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effect-applicable? effects))
   :invalid-params

   :else
   :usable))
