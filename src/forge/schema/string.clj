(ns forge.schema.string
  (:require [forge.editor.widget :as widget]
            [forge.ui :as ui])
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(defmethod widget/create :string [schema v]
  (ui/add-tooltip! (ui/text-field v {})
                   (str schema)))

(defmethod widget/->value :string [_ widget]
  (VisTextField/.getText widget))
