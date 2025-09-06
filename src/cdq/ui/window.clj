(ns cdq.ui.window
  (:require [cdq.ui.table :as table]
            [clojure.vis-ui.window :as window]))

(defn create [opts]
  (-> (window/create opts)
      (table/set-opts! opts)))
