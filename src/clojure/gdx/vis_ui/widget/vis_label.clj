(ns clojure.gdx.vis-ui.widget.vis-label
  (:import (com.kotcrab.vis.ui.widget VisLabel)))

(defn create [text]
  (VisLabel. ^CharSequence (str text)))
