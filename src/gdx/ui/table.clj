(ns gdx.ui.table
  (:require [gdx.ui.actor :as actor]
            [gdx.ui.table.cell :as cell])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Table)))

(defn add! [^Table table actor-or-decl]
  (.add table (actor/construct? actor-or-decl)))

(defn add-rows! [^Table table rows]
  (doseq [row rows]
    (doseq [props-or-actor row]
      (cond
       (map? props-or-actor) (-> (add! table (:actor props-or-actor))
                                 (cell/set-opts! (dissoc props-or-actor :actor)))
       :else (add! table props-or-actor)))
    (.row table))
  table)
