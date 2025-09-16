(ns clojure.vis-ui.label
  (:require [clojure.gdx.scene2d.actor :as actor])
  (:import (com.kotcrab.vis.ui.widget VisLabel)))

(defn create [{:keys [label/text] :as opts}]
  (doto (VisLabel. ^CharSequence text)
    (actor/set-opts! opts)))
