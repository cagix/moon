(ns cdq.ui.editor.widget.enum
  (:require [cdq.ui.editor.widget :as widget]
            [clojure.edn :as edn]
            [gdl.ui :as ui]
            [gdl.utils :refer [->edn-str]]))

(defmethod widget/create :enum [schema v _ctx]
  (ui/select-box {:items (map ->edn-str (rest schema))
                  :selected (->edn-str v)}))

(defmethod widget/value :enum [_ widget _schemas]
  (edn/read-string (ui/get-selected widget)))

