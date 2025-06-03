(ns cdq.ui.editor.widget.string
  (:require [cdq.ui.editor.widget :as widget]
            [gdl.ui :as ui]))

(defmethod widget/create :string [schema v _ctx]
  (ui/add-tooltip! (ui/text-field v {})
                   (str schema)))

(defmethod widget/value :string [_ widget _schemas]
  (ui/get-text widget))
