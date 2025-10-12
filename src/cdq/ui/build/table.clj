(ns cdq.ui.build.table
  (:require [cdq.ui.build.widget-group :as widget-group]
            [clojure.gdx.scene2d.ui.cell :as cell]
            [clojure.gdx.scene2d.ui.table :as gdx-table]
            [cdq.ui.table :as table]
            [clojure.vis-ui.table :as vis-table]))

(extend-type com.badlogic.gdx.scenes.scene2d.ui.Table
  table/Table
  (add! [table actor]
    (gdx-table/add! table actor))

  (cells [table]
    (gdx-table/cells table))

  (add-rows! [table rows]
    (doseq [row rows]
      (doseq [props-or-actor row]
        (cond
         (map? props-or-actor) (-> (table/add! table (:actor props-or-actor))
                                   (cell/set-opts! (dissoc props-or-actor :actor)))
         :else (table/add! table props-or-actor)))
      (gdx-table/row! table))
    table)

  (set-opts! [table {:keys [rows cell-defaults] :as opts}]
    (cell/set-opts! (.defaults table) cell-defaults)
    (doto table
      (table/add-rows! rows)
      (widget-group/set-opts! opts))))

(defn create [opts]
  (-> (vis-table/create)
      (table/set-opts! opts)))
