(ns cdq.ui.image
  (:require [cdq.ui :as ui]
            [clojure.vis-ui.image :as image]))

(defn create
  [object opts]
  (doto (image/create object opts)
    (ui/set-opts! opts)))
