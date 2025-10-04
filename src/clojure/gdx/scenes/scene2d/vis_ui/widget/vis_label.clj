(ns clojure.gdx.scenes.scene2d.vis-ui.widget.vis-label
  (:import (com.kotcrab.vis.ui.widget VisLabel)))

(defn create [text]
  (VisLabel. ^CharSequence (str text)))
