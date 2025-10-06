(ns clojure.scene2d.vis-ui.label
  (:require [clojure.gdx.vis-ui.widget.vis-label :as vis-label]
            [clojure.scene2d.widget :as widget]))

(defn create [{:keys [label/text] :as opts}]
  (doto (vis-label/create text)
    (widget/set-opts! opts)))
