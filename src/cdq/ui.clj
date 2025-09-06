(ns cdq.ui
  (:require [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.table :as table]
            [clojure.gdx.scenes.scene2d.ui.widget-group :as widget-group]))

(defn set-opts! [actor opts]
  (actor/set-opts! actor opts)
  (when (instance? com.badlogic.gdx.scenes.scene2d.ui.Table actor)
    (table/set-opts! actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup actor)
    (widget-group/set-opts! actor opts))
  (when (instance? com.badlogic.gdx.scenes.scene2d.Group actor)
    (group/set-opts! actor opts))
  actor)
