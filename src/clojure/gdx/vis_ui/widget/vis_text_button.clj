(ns clojure.gdx.vis-ui.widget.vis-text-button
  (:import (com.kotcrab.vis.ui.widget VisTextButton)))

(defn create [text]
  (VisTextButton. (str text)))
