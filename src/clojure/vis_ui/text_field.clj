(ns clojure.vis-ui.text-field
  (:require [clojure.gdx.scene2d.ui.widget :as widget]
            [clojure.vis-ui.tooltip :as tooltip])
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(def get-text VisTextField/.getText)

(defn create
  [{:keys [text-field/text]
    :as opts}]
  (let [actor (-> (VisTextField. (str text))
                  (widget/set-opts! opts))]
    (when-let [tooltip (:tooltip opts)]
      (tooltip/add! actor tooltip))
    actor))
