(ns clojure.scene2d.vis-ui.window
  (:require [clojure.scene2d.ui.table :as table])
  (:import (com.kotcrab.vis.ui.widget VisWindow)))

(defn create
  [{:keys [title
           modal?
           close-button?
           center?
           close-on-escape?]
    :as opts}]
  (let [show-window-border? true
        window (let [window (VisWindow. ^String title (boolean show-window-border?))]
                 (when close-button?    (.addCloseButton window))
                 (when center?          (.centerWindow   window))
                 (when close-on-escape? (.closeOnEscape  window))
                 window)]
    (.setModal window (boolean modal?))
    (table/set-opts! window opts)))
