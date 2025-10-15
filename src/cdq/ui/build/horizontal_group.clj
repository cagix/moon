(ns cdq.ui.build.horizontal-group
  (:require [clojure.gdx.scene2d.ui.horizontal-group :as horizontal-group]
            [cdq.ui.group :as group]))

(defn create [opts]
  (doto (horizontal-group/create opts)
    (group/set-opts! opts)))
