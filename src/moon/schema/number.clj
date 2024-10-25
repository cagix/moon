(ns moon.schema.number
  (:require [clojure.edn :as edn]
            [gdl.ui :as ui]
            [gdl.utils :refer [->edn-str]]
            [moon.schema :as schema])
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(defmethod schema/widget number? [schema v]
  (ui/add-tooltip! (ui/text-field (->edn-str v) {})
                   (str schema)))

(defmethod schema/widget-value number? [_ widget]
  (edn/read-string (VisTextField/.getText widget)))
