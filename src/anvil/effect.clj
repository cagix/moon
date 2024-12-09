(ns anvil.effect
  (:require [clojure.utils :refer [defsystem]]))

(defsystem handle [_ ctx])

(defsystem applicable? [_ ctx])

(defsystem useful?          [_  ctx])
(defmethod useful? :default [_ _ctx] true)

(defsystem render-effect           [_  ctx])
(defmethod render-effect :default  [_ _ctx])

(defn effects-applicable? [ctx effects]
  (seq (filter #(applicable? % ctx) effects)))

(defn effects-useful? [ctx effects] ; actually called @ npc idle ... maybe move there ?!
  (->> effects
       (effects-applicable? ctx)
       (some #(useful? % ctx))))

(defn effects-do! [ctx effects]
  (run! #(handle % ctx)
        (effects-applicable? ctx effects)))

(defn effects-render [ctx effects]
  (run! #(render-effect % ctx)
        effects))
