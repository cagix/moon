(ns clojure.vis-ui.text-field
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(defn create [text]
  (VisTextField. (str text)))

(def get-text VisTextField/.getText)
