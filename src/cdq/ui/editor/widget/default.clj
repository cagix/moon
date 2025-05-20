(ns cdq.ui.editor.widget.default
  (:require [cdq.ui.editor.widget :as widget]
            [cdq.utils :refer [truncate
                               ->edn-str]]
            [gdl.ui :as ui]))

(defmethod widget/create :default [_ v _ctx]
  (ui/label (truncate (->edn-str v) 60)))

(defmethod widget/value :default [_ widget _schemas]
  ((ui/user-object widget) 1))
