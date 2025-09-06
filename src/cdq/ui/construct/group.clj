(ns cdq.ui.construct.group
  (:require [cdq.ui :as ui]
            [clojure.gdx.scenes.scene2d.group :as group]))

(defn create [opts]
  (doto (group/create)
    (ui/set-opts! opts)))
