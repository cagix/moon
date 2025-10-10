(ns clojure.scene2d.vis-ui.window
  (:require [clojure.scene2d.ui.table :as table]
            [clojure.vis-ui.window :as window]))

(defn create
  [{:keys [modal?] :as opts}]
  (let [window (window/create opts)]
    (.setModal window (boolean modal?))
    (table/set-opts! window opts)))
