(ns anvil.effect
  (:require [anvil.system :as system]))

(defn applicable? [ctx effects]
  (seq (filter #(system/applicable? % ctx) effects)))

(defn useful? [ctx effects]
  (->> effects
       (applicable? ctx)
       (some #(system/useful? % ctx))))

(defn do! [ctx effects]
  (run! #(system/handle % ctx)
        (applicable? ctx effects)))

(defn render [ctx effects]
  (run! #(system/render-effect % ctx)
        effects))
