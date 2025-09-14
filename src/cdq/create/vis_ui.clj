(ns cdq.create.vis-ui
  (:require [clojure.vis-ui :as vis-ui]
            [clojure.vis-ui.widget]))

(defn do! [ctx]
  (vis-ui/load! (:cdq.vis-ui (:ctx/config ctx)))
  ctx)
