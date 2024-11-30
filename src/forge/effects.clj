(ns forge.effects
  (:require [forge.effect :as effect]))

(defn applicable? [ctx effects]
  (seq (filter #(effect/applicable? % ctx) effects)))

(defn useful? [ctx effects]
  (->> effects
       (applicable? ctx)
       (some #(effect/useful? % ctx))))

(defn do! [ctx effects]
  (run! #(effect/handle % ctx)
        (applicable? ctx effects)))

(defn render [ctx effects]
  (run! #(effect/render % ctx)
        effects))
