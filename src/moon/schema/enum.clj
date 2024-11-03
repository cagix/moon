(ns ^:no-doc moon.schema.enum
  (:require [clojure.edn :as edn]
            [gdl.ui :as ui]
            [gdl.utils :refer [->edn-str]]
            [moon.schema :as schema])
  (:import (com.kotcrab.vis.ui.widget VisSelectBox)))

(defmethod schema/widget :enum [schema v]
  (ui/select-box {:items (map ->edn-str (rest schema))
                  :selected (->edn-str v)}))

(defmethod schema/widget-value :enum [_ widget]
  (edn/read-string (VisSelectBox/.getSelected widget)))
