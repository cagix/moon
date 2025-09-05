(ns cdq.ui.text-field
  (:require [cdq.ui :as ui]
            [cdq.ui.actor :as actor])
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(defmethod actor/construct :actor.type/text-field [{:keys [text-field/text] :as opts}]
  (-> (VisTextField. (str text))
      (ui/set-opts! opts)))

(def get-text VisTextField/.getText)
