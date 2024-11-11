(ns moon.effects
  (:require [moon.effect :as effect]))

(defn- filter-applicable? [ctx effects]
  (filter #(effect/applicable? % ctx) effects))

(defn applicable? [ctx effects]
  (seq (filter-applicable? ctx effects)))

(defn useful? [ctx effects]
  (->> effects
       (filter-applicable? ctx)
       (some #(effect/useful? % ctx))))

(defn do! [ctx effects]
  (doseq [effect (filter-applicable? ctx effects)]
    (effect/handle effect ctx)))
