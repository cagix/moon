(ns cdq.ui.table
  (:require [cdq.construct]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [clojure.gdx.scenes.scene2d.ui.cell :as cell]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.gdx.scenes.scene2d.ui.widget-group :as widget-group]
            [clojure.vis-ui.table :as vis-ui.table]))

(defn add! [table actor-or-decl]
  (table/add! table (cdq.construct/construct? actor-or-decl)))

(def cells table/cells)

(defn add-rows!
  "rows is a seq of seqs of columns.
  Elements are actors or nil (for just adding empty cells ) or a map of
  {:actor :expand? :bottom?  :colspan int :pad :pad-bottom}. Only :actor is required."
  [table rows]
  (doseq [row rows]
    (doseq [props-or-actor row]
      (cond
       (map? props-or-actor) (-> (add! table (:actor props-or-actor))
                                 (cell/set-opts! (dissoc props-or-actor :actor)))
       :else (add! table props-or-actor)))
    (table/row! table))
  table)

(defn set-opts! [table {:keys [rows cell-defaults]}]
  (cell/set-opts! (table/defaults table) cell-defaults)
  (add-rows! table rows))

(defn create [opts]
  (-> (vis-ui.table/create)
      (set-opts! opts)
      (widget-group/set-opts! opts)
      (group/set-opts! opts)
      (actor/set-opts! opts)))
