(ns clojure.gdx.scene2d.ui.table
  (:require [clojure.scene2d :as scene2d]
            [clojure.gdx.scene2d]
            [clojure.gdx.scene2d.ui.cell :as cell])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Table)))

(defn- build? [actor-or-decl]
  (try (cond
        (instance? com.badlogic.gdx.scenes.scene2d.Actor actor-or-decl)
        actor-or-decl
        (nil? actor-or-decl)
        nil
        :else
        (scene2d/build actor-or-decl))
       (catch Throwable t
         (throw (ex-info ""
                         {:actor-or-decl actor-or-decl}
                         t)))))

(defn add! [table actor-or-decl]
  (Table/.add table ^com.badlogic.gdx.scenes.scene2d.Actor (build? actor-or-decl)))

(def cells Table/.getCells)

(defn add-rows!
  [table rows]
  (doseq [row rows]
    (doseq [props-or-actor row]
      (cond
       (map? props-or-actor) (-> (add! table (:actor props-or-actor))
                                 (cell/set-opts! (dissoc props-or-actor :actor)))
       :else (add! table props-or-actor)))
    (Table/.row table))
  table)

(defn set-opts! [table {:keys [rows cell-defaults] :as opts}]
  (cell/set-opts! (Table/.defaults table) cell-defaults)
  (doto table
    (add-rows! rows)
    (clojure.gdx.scene2d/set-widget-group-opts! opts)))
