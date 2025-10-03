(ns clojure.scene2d.vis-ui.window
  (:require [clojure.scene2d.vis-ui.table :as table]
            [com.kotcrab.vis.ui.widget.vis-window :as vis-window]))

(defn create
  [{:keys [title
           modal?
           close-button?
           center?
           close-on-escape?]
    :as opts}]
  (let [window (vis-window/create
                {:title title
                 :close-button? close-button?
                 :center? center?
                 :close-on-escape? close-on-escape?
                 :show-window-border? true})]
    (.setModal window (boolean modal?))
    (table/set-opts! window opts)))
