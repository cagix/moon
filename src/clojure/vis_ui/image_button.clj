(ns clojure.vis-ui.image-button
  (:import (com.kotcrab.vis.ui.widget VisImageButton)))

(defn create [drawable]
  (VisImageButton. drawable))
