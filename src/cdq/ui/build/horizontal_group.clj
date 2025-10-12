(ns cdq.ui.build.horizontal-group
  (:require [cdq.ui.build.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d.ui HorizontalGroup)))

(defn create [{:keys [space pad] :as opts}]
  (doto (let [group (HorizontalGroup.)]
          (when space (.space group (float space)))
          (when pad   (.pad   group (float pad)))
          group)
    (group/set-opts! opts)))
