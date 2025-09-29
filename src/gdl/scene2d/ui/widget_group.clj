(ns gdl.scene2d.ui.widget-group
  (:require [gdl.scene2d.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d.ui WidgetGroup)))

(defn pack! [^WidgetGroup widget-group]
  (.pack widget-group))

(defn set-opts!
  [^WidgetGroup widget-group
   {:keys [fill-parent? pack?]
    :as opts}]
  (.setFillParent widget-group (boolean fill-parent?))
  (when pack? (.pack widget-group))
  (group/set-opts! widget-group opts))
