(ns cdq.ui.label
  (:require [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.vis-ui.label :as label]))

(defn create [{:keys [label/text] :as opts}]
  (doto (label/create text)
    (actor/set-opts! opts)))
