(ns moon.effects
  (:require [moon.effect :as effect]))

(defn filter-applicable? [effects]
  (filter effect/applicable? effects))

(defn applicable? [effects]
  (seq (filter-applicable? effects)))

(defn useful? [effects]
  (->> effects
       filter-applicable?
       (some effect/useful?)))

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

(defn do! [effect-ctx effects]
  (with-ctx effect-ctx
    (doseq [effect (filter-applicable? effects)]
      (effect/handle effect))))
