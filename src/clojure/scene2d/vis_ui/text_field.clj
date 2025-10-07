(ns clojure.scene2d.vis-ui.text-field
  (:require [cdq.ui.tooltip :as tooltip]
            [clojure.scene2d.widget :as widget])
  (:import (clojure.lang ILookup)
           (com.kotcrab.vis.ui.widget VisTextField)))

(defn create
  [{:keys [text-field/text]
    :as opts}]
  (let [actor (-> (proxy [VisTextField ILookup] [(str text)]
                    (valAt [k]
                      (case k
                        :text-field/text (VisTextField/.getText this))))
                  (widget/set-opts! opts))]
    (when-let [tooltip (:tooltip opts)]
      (tooltip/add! actor tooltip))
    actor))
