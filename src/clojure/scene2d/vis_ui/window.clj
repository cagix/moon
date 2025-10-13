(ns clojure.scene2d.vis-ui.window
  (:require [cdq.ui.table :as table]
            [clojure.gdx.scene2d.ui.window :as window]
            [clojure.vis-ui.window :as vis-window]))

(defn create
  [{:keys [modal?] :as opts}]
  (let [window (vis-window/create opts)]
    (window/set-modal! window (boolean modal?))
    (table/set-opts! window opts)))
