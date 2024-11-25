(ns ^:no-doc forge.schema.enum
  (:require [clojure.edn :as edn]
            [forge.editor.widget :as widget]
            [gdl.ui :as ui]
            [gdl.utils :refer [->edn-str]])
  (:import (com.kotcrab.vis.ui.widget VisSelectBox)))

(defmethod widget/create :enum [schema v]
  (ui/select-box {:items (map ->edn-str (rest schema))
                  :selected (->edn-str v)}))

(defmethod widget/->value :enum [_ widget]
  (edn/read-string (VisSelectBox/.getSelected widget)))
