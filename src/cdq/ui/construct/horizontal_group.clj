(ns cdq.ui.construct.horizontal-group
  (:require [cdq.ui :as ui]
            [clojure.gdx.scenes.scene2d.ui.horizontal-group :as horizontal-group]))

(defn create [{:keys [space pad] :as opts}]
  (doto (horizontal-group/create space pad)
    (ui/set-opts! opts)))
