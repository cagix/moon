(ns clojure.scene2d.widget-group
  (:require [clojure.gdx.scenes.scene2d.ui.widget-group :as widget-group]
            [clojure.scene2d.group :as group]))

(defn set-opts!
  [widget-group {:keys [fill-parent? pack?] :as opts}]
  (when fill-parent?
    (widget-group/set-fill-parent! widget-group fill-parent?))
  (when pack?
    (widget-group/pack! widget-group))
  (group/set-opts! widget-group opts))
