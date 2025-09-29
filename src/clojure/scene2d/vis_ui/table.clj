(ns clojure.scene2d.vis-ui.table
  (:require [com.kotcrab.vis.ui.widget.vis-table :as vis-table]
            [clojure.scene2d.ui.table :as table]))

(defn create [opts]
  (-> (vis-table/create)
      (table/set-opts! opts)))
