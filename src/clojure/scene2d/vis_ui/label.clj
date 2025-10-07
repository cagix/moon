(ns clojure.scene2d.vis-ui.label
  (:require [clojure.scene2d.widget :as widget])
  (:import (com.kotcrab.vis.ui.widget VisLabel)))

(defn create [{:keys [label/text] :as opts}]
  (doto (VisLabel. ^CharSequence (str text))
    (widget/set-opts! opts)))
