(ns cdq.effect-context
  (:require [cdq.effect :as effect]))

(defn filter-applicable? [effect-ctx effect]
  (filter #(effect/applicable? % effect-ctx) effect))

(defn some-applicable? [effect-ctx effect]
  (seq (filter-applicable? effect-ctx effect)))

(defn applicable-and-useful? [context effect-ctx effect]
  (->> effect
       (filter-applicable? effect-ctx)
       (some #(effect/useful? % effect-ctx context))))
