(ns clojure.scene2d.build.table
  (:require [com.badlogic.gdx.scenes.scene2d.ui.cell :as cell]
            [clojure.gdx.vis-ui.widget.vis-table :as vis-table]
            [clojure.scene2d.widget-group :as widget-group]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.ui.table :as table])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Table)))

(defn- build? [actor-or-decl]
  (try (cond
        (instance? Actor actor-or-decl)
        actor-or-decl
        (nil? actor-or-decl)
        nil
        :else
        (scene2d/build actor-or-decl))
       (catch Throwable t
         (throw (ex-info ""
                         {:actor-or-decl actor-or-decl}
                         t)))))

(extend-type Table
  table/Table
  (add! [table actor-or-decl]
    (.add table ^Actor (build? actor-or-decl)))

  (cells [table]
    (.getCells table))

  (add-rows! [table rows]
    (doseq [row rows]
      (doseq [props-or-actor row]
        (cond
         (map? props-or-actor) (-> (table/add! table (:actor props-or-actor))
                                   (cell/set-opts! (dissoc props-or-actor :actor)))
         :else (table/add! table props-or-actor)))
      (.row table))
    table)

  (set-opts! [table {:keys [rows cell-defaults] :as opts}]
    (cell/set-opts! (.defaults table) cell-defaults)
    (doto table
      (table/add-rows! rows)
      (widget-group/set-opts! opts))))

(defn create [opts]
  (-> (vis-table/create)
      (table/set-opts! opts)))
