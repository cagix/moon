(ns clojure.scene2d.build.table
  (:require [clojure.gdx.vis-ui.widget.vis-table :as vis-table]
            [clojure.scene2d.widget-group :as widget-group]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.ui.table :as table])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Cell
                                               Table)))

(defn- set-cell-opts! [^Cell cell opts]
  (doseq [[option arg] opts]
    (case option
      :fill-x?    (.fillX     cell)
      :fill-y?    (.fillY     cell)
      :expand?    (.expand    cell)
      :expand-x?  (.expandX   cell)
      :expand-y?  (.expandY   cell)
      :bottom?    (.bottom    cell)
      :colspan    (.colspan   cell (int   arg))
      :pad        (.pad       cell (float arg))
      :pad-top    (.padTop    cell (float arg))
      :pad-bottom (.padBottom cell (float arg))
      :width      (.width     cell (float arg))
      :height     (.height    cell (float arg))
      :center?    (.center    cell)
      :right?     (.right     cell)
      :left?      (.left      cell))))

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
                                   (set-cell-opts! (dissoc props-or-actor :actor)))
         :else (table/add! table props-or-actor)))
      (.row table))
    table)

  (set-opts! [table {:keys [rows cell-defaults] :as opts}]
    (set-cell-opts! (.defaults table) cell-defaults)
    (doto table
      (table/add-rows! rows)
      (widget-group/set-opts! opts))))

(defn create [opts]
  (-> (vis-table/create)
      (table/set-opts! opts)))
