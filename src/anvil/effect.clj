(ns anvil.effect
  (:require [clojure.component :refer [defsystem]]))

(defsystem applicable?)

(defn filter-applicable? [ctx effects]
  (filter #(applicable? % ctx) effects))

(defn some-applicable? [ctx effects]
  (seq (filter-applicable? ctx effects)))

(defsystem handle)

(defn do-all! [ctx effects]
  (run! #(handle % ctx)
        (filter-applicable? ctx effects)))
