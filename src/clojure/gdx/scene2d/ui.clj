(ns clojure.gdx.scene2d.ui
  (:require [clojure.gdx.scene2d :as scene2d]
            [clojure.gdx.scene2d.ui.widget-group :as widget-group])
  (:import (com.badlogic.gdx.scenes.scene2d.ui HorizontalGroup
                                               Stack
                                               Widget)))

(defn horizontal-group [{:keys [space pad] :as opts}]
  (let [group (HorizontalGroup.)]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    (scene2d/set-group-opts! group opts)))

(defmethod scene2d/build :actor.type/horizontal-group [opts]
  (horizontal-group opts))

(defn stack [opts]
  (doto (Stack.)
    (widget-group/set-opts! opts)))

(defmethod scene2d/build :actor.type/stack [opts]
  (stack opts))

(defn widget [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (scene2d/try-draw this f)))))

(defmethod scene2d/build :actor.type/widget [opts]
  (widget opts))
