(ns cdq.ui.editor.widget.enum
  (:require [cdq.ui.editor.widget :as widget]
            [cdq.utils :refer [->edn-str]]
            [clojure.edn :as edn]
            [gdl.ui :as ui]))

(defmethod widget/create :enum [schema v]
  (ui/select-box {:items (map ->edn-str (rest schema))
                  :selected (->edn-str v)}))

(defmethod widget/value :enum [_ widget]
  (edn/read-string (ui/get-selected widget)))

