(ns cdq.ui.horizontal-group
  (:require [cdq.ui.group :as group]
            [clojure.gdx.scenes.scene2d.ui.horizontal-group :as horizontal-group]))

(defn create [{:keys [space pad] :as opts}]
  (doto (horizontal-group/create space pad)
    (group/set-opts! opts)))
