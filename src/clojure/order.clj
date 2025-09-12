(ns clojure.order
  (:require [clojure.persistent-vector :as persistent-vector]))

(defn sort-by-k-order [k-order components]
  (let [max-count (inc (count k-order))]
    (sort-by (fn [[k _]] (or (persistent-vector/index-of k k-order) max-count))
             components)))
