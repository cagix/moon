(ns cdq.ui
  (:require [cdq.ctx :as ctx]
            [cdq.ui.ctx-stage :as ctx-stage]
            [cdq.ui.group :as group]
            [cdq.ui.table :as table]
            [cdq.ui.tooltip :as tooltip]
            [cdq.ui.utils :as utils]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.ui.widget-group :as widget-group]
            clojure.vis-ui.check-box
            [clojure.vis-ui.label :as label]
            [clojure.vis-ui.scroll-pane :as scroll-pane]
            [clojure.vis-ui.table]
            [clojure.vis-ui.window :as window]))

(defn set-opts! [actor opts]
  (actor/set-opts! actor opts)
  (when-let [tooltip (:tooltip opts)]
    (tooltip/add! actor tooltip))
  (when-let [f (:click-listener opts)]
    (.addListener actor (utils/click-listener f)))
  (when (instance? com.badlogic.gdx.scenes.scene2d.ui.Table actor)
    (table/set-opts! actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup actor)
    (widget-group/set-opts! actor opts))
  (when (instance? com.badlogic.gdx.scenes.scene2d.Group actor)
    (run! #(group/add! actor %) (:actors opts)))
  actor)

(defn table [opts]
  (-> (clojure.vis-ui.table/create)
      (set-opts! opts)))

(defn label [{:keys [label/text] :as opts}]
  (doto (label/create text)
    (set-opts! opts)))

(defmethod cdq.construct/create :actor.type/group [opts]
  (doto (clojure.gdx.scenes.scene2d.group/create)
    (set-opts! opts)))

(import 'clojure.lang.MultiFn)
(MultiFn/.addMethod cdq.construct/create :actor.type/label label)
(MultiFn/.addMethod cdq.construct/create :actor.type/table table)
(MultiFn/.addMethod cdq.construct/create :actor.type/check-box clojure.vis-ui.check-box/create)

(defn window [opts]
  (-> (window/create opts)
      (set-opts! opts)))

(defn scroll-pane [actor]
  (doto (scroll-pane/create actor)
    (actor/set-user-object! :scroll-pane)))

; actor was removed -> stage nil -> context nil -> error on text-buttons/etc.
(defn- try-act [actor delta f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   (ctx-stage/get-ctx stage))]
    (f actor delta ctx)))

(defn try-draw [actor f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   (ctx-stage/get-ctx stage))]
    (ctx/handle-draws! ctx (f actor ctx))))

(defmethod cdq.construct/create :actor.type/actor [opts]
  (doto (actor/create
          (fn [this delta]
            (when-let [f (:act opts)]
              (try-act this delta f)))
          (fn [this _batch _parent-alpha]
            (when-let [f (:draw opts)]
              (try-draw this f))))
    (set-opts! opts)))
