(ns gdl.scene2d.vis-ui.label
  (:require [gdl.scene2d.ui.widget :as widget])
  (:import (com.kotcrab.vis.ui.widget VisLabel)))

(defn create [{:keys [label/text] :as opts}]
  (doto (VisLabel. ^CharSequence text)
    (widget/set-opts! opts)))
