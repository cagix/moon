(ns moon.effect
  (:require [moon.component :as component]
            [moon.entity :as entity]))

(defn filter-applicable? [effect]
  (filter component/applicable? effect))

(defn applicable? [effect]
  (seq (filter-applicable? effect)))

(defn useful? [effect]
  (->> effect
       filter-applicable?
       (some component/useful?)))

(declare ^:dynamic source ; always available
         ^:dynamic target ; optional
         ^:dynamic target-direction ; always available ( player? TODO )
         ^:dynamic target-position) ; always available ( player? TODO )

(defmacro with-ctx [ctx & body]
  `(binding [source           (:effect/source           ~ctx)
             target           (:effect/target           ~ctx)
             target-direction (:effect/target-direction ~ctx)
             target-position  (:effect/target-position  ~ctx)]
     ~@body))

(defn skill-usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill}]
  (cond
   cooling-down?
   :cooldown

   (entity/not-enough-mana? entity skill)
   :not-enough-mana

   (not (applicable? effects))
   :invalid-params

   :else
   :usable))
