(ns cdq.ui.editor.widget.string
  (:require [cdq.ui.editor.widget :as widget]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor])
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(defmethod widget/create :string [schema v]
  (actor/add-tooltip! (ui/text-field v {})
                      (str schema)))

(defmethod widget/value :string [_ widget]
  (VisTextField/.getText widget))
