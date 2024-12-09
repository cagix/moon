(ns anvil.effect
  (:require [anvil.system :as system]))

(defn effects-applicable? [ctx effects]
  (seq (filter #(system/applicable? % ctx) effects)))

(defn effects-useful? [ctx effects] ; actually called @ npc idle ... maybe move there ?!
  (->> effects
       (effects-applicable? ctx)
       (some #(system/useful? % ctx))))

(defn effects-do! [ctx effects]
  (run! #(system/handle % ctx)
        (effects-applicable? ctx effects)))

(defn effects-render [ctx effects]
  (run! #(system/render-effect % ctx)
        effects))
