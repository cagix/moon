(ns clojure.scene2d.vis-ui.text-field
  (:require [cdq.ui.tooltip :as tooltip]
            [clojure.scene2d.widget :as widget])
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(defn create
  [{:keys [text-field/text]
    :as opts}]
  (let [actor (-> (VisTextField. (str text))
                  (widget/set-opts! opts))]
    (when-let [tooltip (:tooltip opts)]
      (tooltip/add! actor tooltip))
    actor))

(def text VisTextField/.getText)
