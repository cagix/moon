(ns clojure.gdx.scene2d.ui.table
  (:require [clojure.gdx.scene2d]
            [clojure.gdx.scene2d.ui.cell :as cell]
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
  clojure.scene2d.ui.table/Table
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
    table))

(defn set-opts! [table {:keys [rows cell-defaults] :as opts}]
  (cell/set-opts! (Table/.defaults table) cell-defaults)
  (doto table
    (table/add-rows! rows)
    (clojure.gdx.scene2d/set-widget-group-opts! opts)))
