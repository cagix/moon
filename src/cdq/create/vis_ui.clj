(ns cdq.create.vis-ui
  (:require [clojure.vis-ui :as vis-ui]))

(defn do!
  [_ctx params]
  (vis-ui/load! params)
  nil)
