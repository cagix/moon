(ns clojure.gdx.scene2d
  (:require [cdq.start]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.ui.table :as table]
            [clojure.gdx.scene2d.group :as group]
            [clojure.walk :as walk]
            [com.badlogic.gdx.scenes.scene2d.ui.cell :as cell])
  (:import (clojure.lang MultiFn)
           (com.badlogic.gdx.scenes.scene2d Actor)
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

(def impls (walk/postwalk
            cdq.start/require-resolve-symbols
            '[[clojure.scene2d/build
               :actor.type/actor
               clojure.gdx.scene2d.actor/create]

              [clojure.scene2d/build
               :actor.type/group
               clojure.gdx.scene2d.group/create]

              [clojure.scene2d/build
               :actor.type/widget
               clojure.gdx.scene2d.actor/create-widget]

              [clojure.scene2d/build
               :actor.type/menu-bar
               clojure.gdx.scene2d.actor.menu-bar/create]

              [clojure.scene2d/build
               :actor.type/select-box
               clojure.vis-ui.select-box/create]

              [clojure.scene2d/build
               :actor.type/label
               clojure.vis-ui.label/create]

              [clojure.scene2d/build
               :actor.type/text-field
               clojure.vis-ui.text-field/create]

              [clojure.scene2d/build
               :actor.type/check-box
               clojure.vis-ui.check-box/create]

              [clojure.scene2d/build
               :actor.type/table
               clojure.vis-ui.table/create]

              [clojure.scene2d/build
               :actor.type/image-button
               clojure.vis-ui.image-button/create]

              [clojure.scene2d/build
               :actor.type/text-button
               clojure.vis-ui.text-button/create]

              [clojure.scene2d/build
               :actor.type/window
               clojure.vis-ui.window/create]

              [clojure.scene2d/build
               :actor.type/image
               clojure.vis-ui.image/create]]))

(defn init! [ctx]
  (doseq [[defmulti-var k method-fn-var] impls]
    (assert (var? defmulti-var))
    (assert (keyword? k))
    (assert (var? method-fn-var))
    (MultiFn/.addMethod @defmulti-var k method-fn-var))
  ctx)
