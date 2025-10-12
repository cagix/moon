(ns clojure.scene2d.vis-ui.image
  (:require [cdq.ui.build.widget :as widget]
            [clojure.vis-ui.image :as image])
  (:import (com.badlogic.gdx.utils Align
                                   Scaling)))

(defn create
  [{:keys [image/object
           scaling
           align
           fill-parent?]
    :as opts}]
  (let [image (image/create object)]
    (when (= :center align)
      (.setAlign image Align/center))
    (when (= :fill scaling)
      (.setScaling image Scaling/fill))
    (when fill-parent?
      (.setFillParent image true))
    (widget/set-opts! image opts)))
