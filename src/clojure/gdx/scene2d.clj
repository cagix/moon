(ns clojure.gdx.scene2d
  (:require [clojure.scene2d :as scene2d]
            [clojure.gdx.scene2d.group :as group]
            [clojure.scene2d.ui.table :as table]
            [com.badlogic.gdx.scenes.scene2d.ui.cell :as cell])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui HorizontalGroup
                                               Stack
                                               Table
                                               WidgetGroup)))

(defn set-widget-group-opts!
  [^WidgetGroup widget-group
   {:keys [fill-parent? pack?]
    :as opts}]
  (.setFillParent widget-group (boolean fill-parent?)) ; <- actor? TODO
  (when pack?
    (.pack widget-group))
  (group/set-opts! widget-group opts))

(comment
 ; fill parent & pack is from Widget TODO ( not widget-group ?)
 com.badlogic.gdx.scenes.scene2d.ui.Widget
 ; about .pack :
 ; Generally this method should not be called in an actor's constructor because it calls Layout.layout(), which means a subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size to Layout.getPrefWidth() and Layout.getPrefHeight(). This allows the actor to have a size at construction time for more convenient use with groups that do not layout their children.
 )

(defn- horizontal-group [{:keys [space pad] :as opts}]
  (let [group (HorizontalGroup.)]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    (group/set-opts! group opts)))

(defmethod scene2d/build :actor.type/horizontal-group [opts]
  (horizontal-group opts))

(defn- stack [opts]
  (doto (Stack.)
    (set-widget-group-opts! opts)))

(defmethod scene2d/build :actor.type/stack [opts]
  (stack opts))

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
    table)

  (set-opts! [table {:keys [rows cell-defaults] :as opts}]
    (cell/set-opts! (.defaults table) cell-defaults)
    (doto table
      (table/add-rows! rows)
      (set-widget-group-opts! opts))))
