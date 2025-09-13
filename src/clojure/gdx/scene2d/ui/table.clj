(ns clojure.gdx.scene2d.ui.table
  (:require [clojure.gdx.scene2d :as scene2d]
            [clojure.gdx.scene2d.ui.cell :as cell]
            [clojure.gdx.scene2d.ui.widget-group :as widget-group])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Table)))

(defn- build? [actor-or-decl]
  (cond
   (instance? com.badlogic.gdx.scenes.scene2d.Actor actor-or-decl)
   actor-or-decl
   (nil? actor-or-decl)
   nil
   :else
   (scene2d/build actor-or-decl)))

(defn add! [table actor-or-decl]
  (Table/.add table ^com.badlogic.gdx.scenes.scene2d.Actor (build? actor-or-decl)))

(def cells Table/.getCells)

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
    (Table/.row table))
  table)

(defn set-opts! [table {:keys [rows cell-defaults] :as opts}]
  (cell/set-opts! (Table/.defaults table) cell-defaults)
  (doto table
    (add-rows! rows)
    (widget-group/set-opts! opts)))
