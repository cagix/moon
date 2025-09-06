(ns cdq.ui.widget-group
  (:require [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.ui.widget-group :as widget-group]))

(defn set-opts! [widget-group opts]
  (doto widget-group
    (widget-group/set-opts! opts)
    (group/set-opts! opts)))
