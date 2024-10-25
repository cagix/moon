(ns moon.schema.string
  (:require [gdl.ui :as ui]
            [moon.schema :as schema])
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(defmethod schema/widget :string [schema v]
  (ui/add-tooltip! (ui/text-field v {})
                   (str schema)))

(defmethod schema/widget-value :string [_ widget]
  (VisTextField/.getText widget))
