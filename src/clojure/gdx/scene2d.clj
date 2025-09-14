(ns clojure.gdx.scene2d
  (:require [cdq.ctx.graphics :as graphics]
            [clojure.scene2d :as scene2d]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.ctx-stage :as ctx-stage])
  (:import (com.badlogic.gdx.scenes.scene2d Group)
           (com.badlogic.gdx.scenes.scene2d.ui HorizontalGroup
                                               Stack
                                               Widget
                                               WidgetGroup)))

(defn- try-act [actor delta f]
  (when-let [stage (actor/get-stage actor)]
    (f actor delta (ctx-stage/get-ctx stage))))

(defn try-draw [actor f]
  (when-let [stage (actor/get-stage actor)]
    (let [ctx (ctx-stage/get-ctx stage)]
      (graphics/handle-draws! (:ctx/graphics ctx)
                              (f actor ctx)))))

(defn- actor
  [{:keys [act draw]
    :as opts}]
  (actor/create
   (assoc opts
          :actor/act (fn [actor delta]
                       (when act
                         (try-act actor delta act)))
          :actor/draw (fn [actor _batch _parent-alpha]
                        (when draw
                          (try-draw actor draw))))))

(defmethod scene2d/build :actor.type/actor [opts] (actor opts))

(defn set-group-opts! [group opts]
  (run! (fn [actor-or-decl]
          ; remove if
          ; inventory widget/image
          ; inventory itself widget/window
          ; cdq.ui.windows entity info window
          (group/add! group (if (instance? com.badlogic.gdx.scenes.scene2d.Actor actor-or-decl)
                              actor-or-decl
                              (scene2d/build actor-or-decl))))
        (:group/actors opts))
  (actor/set-opts! group opts))

(defn- group [opts]
  (doto (Group.)
    (set-group-opts! opts)))

(defmethod scene2d/build :actor.type/group [opts] (group opts))

(defn set-widget-group-opts!
  [^WidgetGroup widget-group
   {:keys [fill-parent? pack?]
    :as opts}]
  (.setFillParent widget-group (boolean fill-parent?)) ; <- actor? TODO
  (when pack?
    (.pack widget-group))
  (set-group-opts! widget-group opts))

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
    (set-group-opts! group opts)))

(defmethod scene2d/build :actor.type/horizontal-group [opts]
  (horizontal-group opts))

(defn- stack [opts]
  (doto (Stack.)
    (set-widget-group-opts! opts)))

(defmethod scene2d/build :actor.type/stack [opts]
  (stack opts))

(defn- widget [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (clojure.gdx.scene2d/try-draw this f)))))

(defmethod scene2d/build :actor.type/widget [opts]
  (widget opts))
