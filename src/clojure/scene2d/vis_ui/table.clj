(ns clojure.scene2d.vis-ui.table
  (:require [com.badlogic.gdx.scenes.scene2d.ui.cell :as cell]
            [com.badlogic.gdx.scenes.scene2d.ui.table :as table]
            [com.badlogic.gdx.scenes.scene2d.ui.widget-group :as widget-group]
            [com.kotcrab.vis.ui.widget.vis-table :as vis-table]))

(defn set-opts! [table {:keys [rows cell-defaults] :as opts}]
  (cell/set-opts! (.defaults table) cell-defaults)
  (doto table
    (table/add-rows! rows)
    (widget-group/set-opts! opts)))

(defn create [opts]
  (-> (vis-table/create)
      (set-opts! opts)))
