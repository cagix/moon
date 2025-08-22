(ns gdx.ui.table
  (:require [gdx.ui.actor :as actor]
            [gdx.ui.table.cell :as cell])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Table)))

(defn add! [^Table table actor-or-decl]
  (.add table (actor/construct? actor-or-decl)))

(defn cells [^Table table]
  (.getCells table))

(defn add-rows!
  "rows is a seq of seqs of columns.
  Elements are actors or nil (for just adding empty cells ) or a map of
  {:actor :expand? :bottom?  :colspan int :pad :pad-bottom}. Only :actor is required."
  [^Table table rows]
  (doseq [row rows]
    (doseq [props-or-actor row]
      (cond
       (map? props-or-actor) (-> (add! table (:actor props-or-actor))
                                 (cell/set-opts! (dissoc props-or-actor :actor)))
       :else (add! table props-or-actor)))
    (.row table))
  table)

(defn set-opts! [^Table table {:keys [rows cell-defaults]}]
  (cell/set-opts! (.defaults table) cell-defaults)
  (add-rows! table rows))
