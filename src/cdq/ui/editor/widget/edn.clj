(ns cdq.ui.editor.widget.edn
  (:require [cdq.ui.editor.widget :as widget]
            [cdq.utils :refer [->edn-str]]
            [clojure.edn :as edn]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor])
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(defmethod widget/create :widget/edn [schema v]
  (actor/add-tooltip! (ui/text-field (->edn-str v) {})
                      (str schema)))

(defmethod widget/value :widget/edn [_ widget]
  (edn/read-string (VisTextField/.getText widget)))

