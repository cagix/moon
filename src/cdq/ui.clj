(ns cdq.ui
  (:require [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.table :as table]
            [clojure.gdx.scenes.scene2d.ui.widget-group :as widget-group]
            [clojure.vis-ui.label :as label]))

(defn set-opts! [actor opts]
  (actor/set-opts! actor opts)
  (when (instance? com.badlogic.gdx.scenes.scene2d.ui.Table actor)
    (table/set-opts! actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup actor)
    (widget-group/set-opts! actor opts))
  (when (instance? com.badlogic.gdx.scenes.scene2d.Group actor)
    (group/set-opts! actor opts))
  actor)

(defn label [{:keys [label/text] :as opts}]
  (doto (label/create text)
    (set-opts! opts)))

(import 'clojure.lang.MultiFn)
(MultiFn/.addMethod cdq.construct/create :actor.type/label label)
