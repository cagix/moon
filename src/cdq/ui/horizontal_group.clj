(ns cdq.ui.horizontal-group
  (:require [cdq.ui.group :as group]
            [clojure.gdx.scenes.scene2d.ui :as ui]))

(defn create [{:keys [space pad] :as opts}]
  (doto (ui/horizontal-group space pad)
    (group/set-opts! opts)))
