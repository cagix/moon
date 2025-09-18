(ns com.kotcrab.vis.ui.widget.text-field
  (:require [clojure.scene2d.ui.text-field]
            [clojure.scene2d.ui.widget :as widget]
            [com.kotcrab.vis.ui.widget.tooltip :as tooltip])
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(extend-type VisTextField
  clojure.scene2d.ui.text-field/TextField
  (get-text [this]
    (.getText this)))

(defn create
  [{:keys [text-field/text]
    :as opts}]
  (let [actor (-> (VisTextField. (str text))
                  (widget/set-opts! opts))]
    (when-let [tooltip (:tooltip opts)]
      (tooltip/add! actor tooltip))
    actor))
