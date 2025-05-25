(ns cdq.ui.editor.widget.edn
  (:require [cdq.ui.editor.widget :as widget]
            [clojure.edn :as edn]
            [gdl.ui :as ui]
            [gdl.utils :refer [->edn-str]]))

(defmethod widget/create :widget/edn [schema v _ctx]
  (ui/add-tooltip! (ui/text-field (->edn-str v) {})
                   (str schema)))

(defmethod widget/value :widget/edn [_ widget _schemas]
  (edn/read-string (ui/get-text widget)))

