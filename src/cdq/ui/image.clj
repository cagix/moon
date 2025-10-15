(ns cdq.ui.image
  (:require [cdq.ui.actor :as actor]
            [clojure.gdx.scene2d.ui.image :as image]
            [clojure.gdx.scene2d.ui.widget :as widget]
            [clojure.gdx.utils.align :as align]
            [clojure.gdx.utils.scaling :as scaling]
            [clojure.vis-ui.image :as vis-image]))

(defn create
  [{:keys [image/object
           scaling
           align
           fill-parent?]
    :as opts}]
  (let [image (vis-image/create object)]
    (when (= :center align)
      (image/set-align! image align/center))
    (when (= :fill scaling)
      (image/set-scaling! image scaling/fill))
    (when fill-parent?
      (widget/set-fill-parent! image true))
    (actor/set-opts! image opts)))
