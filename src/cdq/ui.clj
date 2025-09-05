(ns cdq.ui
  (:require [cdq.ctx :as ctx]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.table :as table]
            [cdq.ui.utils :as utils]
            [cdq.ui.widget-group :as widget-group])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)
           (com.badlogic.gdx.scenes.scene2d.ui Button
                                               Table
                                               VerticalGroup
                                               WidgetGroup)
           (com.kotcrab.vis.ui.widget VisLabel
                                      VisScrollPane
                                      VisTable
                                      VisTextButton
                                      VisWindow)))

(defn set-opts! [actor opts]
  (actor/set-opts! actor opts)
  (when (instance? Table actor)
    (table/set-opts! actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (widget-group/set-opts! actor opts))
  (when (instance? Group actor)
    (run! #(group/add! actor %) (:actors opts)))
  actor)

(defn table ^Table [opts]
  (-> (group/proxy-ILookup VisTable [])
      (set-opts! opts)))

(defn label ^VisLabel [{:keys [label/text] :as opts}]
  (doto (VisLabel. ^CharSequence text)
    (set-opts! opts)))

(defmethod actor/construct :actor.type/group [opts]
  (doto (cdq.ui.group/proxy-ILookup Group [])
    (set-opts! opts)))

#_(defn- -vertical-group [actors]
    (let [group (cdq.ui.group/proxy-ILookup VerticalGroup [])]
      (run! #(group/add! group %) actors) ; redundant if we use map based
      group))

(import 'clojure.lang.MultiFn)
(MultiFn/.addMethod actor/construct :actor.type/label label)
(MultiFn/.addMethod actor/construct :actor.type/table table)

(defn window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (cdq.ui.group/proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
                     (.setModal (boolean modal?)))]
        (when close-button?    (.addCloseButton window))
        (when center?          (.centerWindow   window))
        (when close-on-escape? (.closeOnEscape  window))
        window)
      (set-opts! opts)))

(defn scroll-pane [actor]
  (doto (VisScrollPane. actor)
    (actor/set-user-object! :scroll-pane)
    (.setFlickScroll false)
    (.setFadeScrollBars false)))

(defn text-button [text on-clicked]
  (doto (VisTextButton. (str text))
    (.addListener (utils/change-listener on-clicked))))

; actor was removed -> stage nil -> context nil -> error on text-buttons/etc.
(defn- try-act [actor delta f]
  (when-let [ctx (actor/get-stage-ctx actor)]
    (f actor delta ctx)))

(defn try-draw [actor f]
  (when-let [ctx (actor/get-stage-ctx actor)]
    (ctx/handle-draws! ctx (f actor ctx))))

(defmethod actor/construct :actor.type/actor [opts]
  (doto (proxy [Actor] []
          (act [delta]
            (when-let [f (:act opts)]
              (try-act this delta f)))
          (draw [_batch _parent-alpha]
            (when-let [f (:draw opts)]
              (try-draw this f))))
    (actor/set-opts! opts)))
