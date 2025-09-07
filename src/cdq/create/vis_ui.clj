(ns cdq.create.vis-ui
  (:require [clojure.vis-ui :as vis-ui]))

(defn do!
  [_ctx]
  (vis-ui/load! {:skin-scale :x1})
  nil)
