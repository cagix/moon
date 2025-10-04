(ns clojure.scene2d.vis-ui.image
  (:require [clojure.gdx.scenes.scene2d.vis-ui.widget.vis-image :as vis-image]
            [clojure.scene2d.widget :as widget])
  (:import (com.badlogic.gdx.utils Align
                                   Scaling)))


(defn create
  [{:keys [image/object
           scaling
           align
           fill-parent?]
    :as opts}]
  (let [image (vis-image/create object)]
    (when (= :center align)
      (.setAlign image Align/center))
    (when (= :fill scaling)
      (.setScaling image Scaling/fill))
    (when fill-parent?
      (.setFillParent image true))
    (widget/set-opts! image opts)))
